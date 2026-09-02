package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneItemScoreDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.dorm.repository.ResidentAbsenceSnapshot;
import edu.seu.vcampus.server.dorm.service.DormHygieneRules;
import edu.seu.vcampus.server.dorm.service.DormStayRules;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/**
 * 抄表读数的 MySQL 实现。
 *
 * <p>与既有宿舍 DAO 放在同一个包，直接复用 {@code JdbcDormSupport} 的分页和时间转换，
 * 不需要复制一份工具代码，也不需要改动原有类。</p>
 */
public final class MySqlDormExtRepository implements DormExtRepository {
    private static final String COLUMNS =
            "m.id, m.room_id, b.building_code, r.room_no, m.period_start, m.period_end,"
            + " m.electricity_units, m.water_units, m.electricity_price, m.water_price,"
            + " m.recorded_by, m.recorded_at, m.bill_id";
    private static final String FROM =
            " FROM dorm_meter_readings m"
            + " JOIN dorm_rooms r ON r.id = m.room_id"
            + " JOIN dorm_buildings b ON b.id = r.building_id";

    @Override
    public DormPage<MeterReadingDto> listMeterReadings(Connection c, DormPageQuery query)
            throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q.getRoomId() != null) {
            where.append(" AND m.room_id = ?");
            params.add(q.getRoomId());
        }
        if (q.getBuildingId() != null) {
            where.append(" AND r.building_id = ?");
            params.add(q.getBuildingId());
        }
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) {
            where.append(" AND (r.room_no LIKE ? OR b.building_code LIKE ? OR b.building_name LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        String countSql = "SELECT COUNT(*)" + FROM + where;
        String dataSql = "SELECT " + COLUMNS + FROM + where
                + " ORDER BY m.period_start DESC, m.id DESC LIMIT ? OFFSET ?";
        return JdbcDormSupport.page(c, countSql, dataSql, params, q.getPage(), q.getPageSize(),
                new JdbcDormSupport.Reader<MeterReadingDto>() {
                    @Override
                    public MeterReadingDto read(ResultSet result) throws SQLException {
                        return meterReading(result);
                    }
                });
    }

    @Override
    public MeterReadingDto saveMeterReading(Connection c, MeterReadingRequest request,
                                            long actorUserId) throws SQLException {
        ensureRoom(c, request.getRoomId());
        long existing = findId(c, request);
        if (existing > 0L) {
            update(c, existing, request, actorUserId);
            return require(c, existing);
        }
        return require(c, insert(c, request, actorUserId));
    }

    /** 房间不存在时给出业务错误，避免外键异常直接抛给客户端。 */
    private static void ensureRoom(Connection c, long roomId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM dorm_rooms WHERE id = ?")) {
            s.setLong(1, roomId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) {
                    throw new DormRepositoryException(DormExtCommands.ROOM_NOT_FOUND, "房间不存在");
                }
            }
        }
    }

    /** 同房间同账期唯一；顺带校验是否已被账单占用。 */
    private static long findId(Connection c, MeterReadingRequest request) throws SQLException {
        String sql = "SELECT id, bill_id FROM dorm_meter_readings"
                + " WHERE room_id = ? AND period_start = ? AND period_end = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, request.getRoomId());
            s.setDate(2, JdbcTemporal.date(request.getPeriodStart()));
            s.setDate(3, JdbcTemporal.date(request.getPeriodEnd()));
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return 0L;
                long id = r.getLong("id");
                r.getLong("bill_id");
                if (!r.wasNull()) {
                    throw new DormRepositoryException(DormExtCommands.METER_LOCKED,
                            "该账期已生成账单，读数不可修改");
                }
                return id;
            }
        }
    }

    private static long insert(Connection c, MeterReadingRequest request, long actorUserId)
            throws SQLException {
        String sql = "INSERT INTO dorm_meter_readings(room_id,period_start,period_end,"
                + "electricity_units,water_units,electricity_price,water_price,recorded_by)"
                + " VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, request.getRoomId());
            s.setDate(2, JdbcTemporal.date(request.getPeriodStart()));
            s.setDate(3, JdbcTemporal.date(request.getPeriodEnd()));
            bindAmounts(s, request, 4);
            s.setLong(8, actorUserId);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("meter reading id was not generated");
                return keys.getLong(1);
            }
        }
    }

    private static void update(Connection c, long id, MeterReadingRequest request, long actorUserId)
            throws SQLException {
        String sql = "UPDATE dorm_meter_readings SET electricity_units = ?, water_units = ?,"
                + " electricity_price = ?, water_price = ?, recorded_by = ?"
                + " WHERE id = ? AND bill_id IS NULL";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bindAmounts(s, request, 1);
            s.setLong(5, actorUserId);
            s.setLong(6, id);
            if (s.executeUpdate() == 0) {
                throw new DormRepositoryException(DormExtCommands.METER_LOCKED,
                        "该账期已生成账单，读数不可修改");
            }
        }
    }

    private static void bindAmounts(PreparedStatement s, MeterReadingRequest request, int from)
            throws SQLException {
        s.setBigDecimal(from, request.getElectricityUnits());
        s.setBigDecimal(from + 1, request.getWaterUnits());
        s.setBigDecimal(from + 2, request.getElectricityPrice());
        s.setBigDecimal(from + 3, request.getWaterPrice());
    }

    private static MeterReadingDto require(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE m.id = ?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) {
                    throw new DormRepositoryException(DormExtCommands.METER_NOT_FOUND, "抄表读数不存在");
                }
                return meterReading(r);
            }
        }
    }

    @Override
    public List<MeterReadingDto> pendingReadings(Connection c, Long roomId,
                                                 LocalDate periodStart, LocalDate periodEnd)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(FROM)
                .append(" WHERE m.bill_id IS NULL AND m.period_start = ? AND m.period_end = ?");
        if (roomId != null) sql.append(" AND m.room_id = ?");
        sql.append(" ORDER BY m.room_id");
        List<MeterReadingDto> rows = new ArrayList<MeterReadingDto>();
        try (PreparedStatement s = c.prepareStatement(sql.toString())) {
            s.setDate(1, JdbcTemporal.date(periodStart));
            s.setDate(2, JdbcTemporal.date(periodEnd));
            if (roomId != null) s.setLong(3, roomId.longValue());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(meterReading(r));
            }
        }
        return rows;
    }

    @Override
    public List<Long> activeResidents(Connection c, long roomId) throws SQLException {
        String sql = "SELECT ar.student_user_id FROM accommodation_records ar"
                + " JOIN dorm_beds bd ON bd.id = ar.bed_id"
                + " WHERE bd.room_id = ? AND ar.status = 'ACTIVE'"
                + " ORDER BY ar.student_user_id";
        List<Long> rows = new ArrayList<Long>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, roomId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(Long.valueOf(r.getLong(1)));
            }
        }
        return rows;
    }

    @Override
    public boolean billExists(Connection c, long roomId, LocalDate periodStart,
                              LocalDate periodEnd) throws SQLException {
        String sql = "SELECT 1 FROM utility_bills"
                + " WHERE room_id = ? AND period_start = ? AND period_end = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, roomId);
            s.setDate(2, JdbcTemporal.date(periodStart));
            s.setDate(3, JdbcTemporal.date(periodEnd));
            try (ResultSet r = s.executeQuery()) {
                return r.next();
            }
        }
    }

    @Override
    public long createBill(Connection c, MeterReadingDto reading, BigDecimal totalAmount,
                           LocalDateTime dueAt, long actorUserId) throws SQLException {
        String sql = "INSERT INTO utility_bills(room_id,period_start,period_end,"
                + "electricity_units,water_units,total_amount,due_at,status,created_by)"
                + " VALUES(?,?,?,?,?,?,?,'UNPAID',?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, reading.getRoomId());
            s.setDate(2, JdbcTemporal.date(reading.getPeriodStart()));
            s.setDate(3, JdbcTemporal.date(reading.getPeriodEnd()));
            s.setBigDecimal(4, reading.getElectricityUnits());
            s.setBigDecimal(5, reading.getWaterUnits());
            s.setBigDecimal(6, totalAmount);
            s.setTimestamp(7, JdbcTemporal.timestamp(dueAt));
            s.setLong(8, actorUserId);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("utility bill id was not generated");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public void createAllocation(Connection c, long billId, long studentUserId,
                                 BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO utility_allocations(bill_id,student_user_id,amount,status)"
                + " VALUES(?,?,?,'UNPAID')";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, billId);
            s.setLong(2, studentUserId);
            s.setBigDecimal(3, amount);
            s.executeUpdate();
        }
    }

    @Override
    public void linkReadingToBill(Connection c, long readingId, long billId) throws SQLException {
        String sql = "UPDATE dorm_meter_readings SET bill_id = ? WHERE id = ? AND bill_id IS NULL";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, billId);
            s.setLong(2, readingId);
            if (s.executeUpdate() == 0) {
                throw new DormRepositoryException(DormExtCommands.METER_LOCKED,
                        "抄表读数已被其他账单占用");
            }
        }
    }

    // ================= 在宿状态、门禁策略、入内许可、房间删除 =================

    @Override
    public List<StayStatusDto> stayStatusRows(Connection c, Long studentUserId)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT ar.student_user_id, bd.room_id, b.building_code, r.room_no,"
                + " (SELECT MAX(x.occurred_at) FROM access_records x"
                + "   WHERE x.student_user_id = ar.student_user_id AND x.record_type = 'EXIT')"
                + "  AS last_exit_at,"
                + " (SELECT MAX(x.occurred_at) FROM access_records x"
                + "   WHERE x.student_user_id = ar.student_user_id AND x.record_type = 'ENTRY')"
                + "  AS last_entry_at"
                + " FROM accommodation_records ar"
                + " JOIN dorm_beds bd ON bd.id = ar.bed_id"
                + " JOIN dorm_rooms r ON r.id = bd.room_id"
                + " JOIN dorm_buildings b ON b.id = r.building_id"
                + " WHERE ar.status = 'ACTIVE'");
        if (studentUserId != null) sql.append(" AND ar.student_user_id = ?");
        sql.append(" ORDER BY ar.student_user_id");
        List<StayStatusDto> rows = new ArrayList<StayStatusDto>();
        try (PreparedStatement s = c.prepareStatement(sql.toString())) {
            if (studentUserId != null) s.setLong(1, studentUserId.longValue());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    rows.add(new StayStatusDto(r.getLong("student_user_id"), r.getLong("room_id"),
                            r.getString("building_code"), r.getString("room_no"), null,
                            JdbcDormSupport.localTimestamp(r, "last_exit_at"),
                            JdbcDormSupport.localTimestamp(r, "last_entry_at")));
                }
            }
        }
        return rows;
    }

    @Override
    public AccessPolicyDto loadAccessPolicy(Connection c) throws SQLException {
        String sql = "SELECT curfew_time, dawn_time, updated_at FROM dorm_access_policies"
                + " WHERE id = 1";
        try (PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            if (!r.next()) {
                throw new DormRepositoryException(DormExtCommands.POLICY_INVALID,
                        "门禁策略未初始化，请先执行 V3 迁移");
            }
            return new AccessPolicyDto(JdbcTemporal.localTime(r.getTime("curfew_time")),
                    JdbcTemporal.localTime(r.getTime("dawn_time")),
                    JdbcDormSupport.localTimestamp(r, "updated_at"));
        }
    }

    @Override
    public AccessPolicyDto saveAccessPolicy(Connection c, AccessPolicyRequest request,
                                            long actorUserId) throws SQLException {
        String sql = "UPDATE dorm_access_policies SET curfew_time = ?, dawn_time = ?,"
                + " updated_by = ? WHERE id = 1";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setTime(1, JdbcTemporal.time(request.getCurfewTime()));
            s.setTime(2, JdbcTemporal.time(request.getDawnTime()));
            s.setLong(3, actorUserId);
            s.executeUpdate();
        }
        return loadAccessPolicy(c);
    }

    @Override
    public DormPage<AccessRecordExtDto> listAccessRecords(Connection c, long studentUserId,
                                                          DormPageQuery query,
                                                          final AccessPolicyDto policy)
            throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        params.add(Long.valueOf(studentUserId));
        StringBuilder where = new StringBuilder(" WHERE a.student_user_id = ?");
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) {
            where.append(" AND (a.door_name LIKE ? OR a.record_type LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
        }
        // status 复用成进出类型过滤：客户端的「仅入宿／仅离宿」下拉走这里，
        // 取值受 ck_access_records_type 约束，非法值查不到数据而不是报错。
        String type = JdbcDormSupport.clean(q.getStatus());
        if (type != null) {
            where.append(" AND a.record_type = ?");
            params.add(type);
        }
        String from = " FROM access_records a";
        String countSql = "SELECT COUNT(*)" + from + where;
        String dataSql = "SELECT a.id, a.student_user_id, a.record_type, a.occurred_at,"
                + " a.door_name" + from + where
                + " ORDER BY a.occurred_at DESC, a.id DESC LIMIT ? OFFSET ?";
        final LocalTime curfew = policy.getCurfewTime();
        final LocalTime dawn = policy.getDawnTime();
        return JdbcDormSupport.page(c, countSql, dataSql, params, q.getPage(), q.getPageSize(),
                new JdbcDormSupport.Reader<AccessRecordExtDto>() {
                    @Override
                    public AccessRecordExtDto read(ResultSet r) throws SQLException {
                        String type = r.getString("record_type");
                        LocalDateTime at = JdbcDormSupport.localTimestamp(r, "occurred_at");
                        return new AccessRecordExtDto(r.getLong("id"),
                                r.getLong("student_user_id"), type, at, r.getString("door_name"),
                                DormStayRules.isLateReturn(type, at, curfew, dawn));
                    }
                });
    }

    @Override
    public Long repairReporterOf(Connection c, long repairOrderId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT reporter_id FROM repair_orders WHERE id = ?")) {
            s.setLong(1, repairOrderId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Long.valueOf(r.getLong(1)) : null;
            }
        }
    }

    @Override
    public RepairEntryPermitDto saveRepairPermit(Connection c, RepairEntryPermitRequest request,
                                                 long actorUserId) throws SQLException {
        String sql = "INSERT INTO dorm_repair_entry_permits(repair_order_id,allow_enter,note,"
                + "updated_by) VALUES(?,?,?,?) AS incoming"
                + " ON DUPLICATE KEY UPDATE allow_enter = incoming.allow_enter,"
                + " note = incoming.note, updated_by = incoming.updated_by";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, request.getRepairOrderId());
            s.setBoolean(2, request.isAllowEnter());
            s.setString(3, JdbcDormSupport.clean(request.getNote()));
            s.setLong(4, actorUserId);
            s.executeUpdate();
        }
        try (PreparedStatement s = c.prepareStatement(PERMIT_SELECT + PERMIT_FROM
                + " WHERE o.id = ?")) {
            s.setLong(1, request.getRepairOrderId());
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) {
                    throw new DormRepositoryException(DormExtCommands.REPAIR_NOT_FOUND, "报修单不存在");
                }
                return permit(r);
            }
        }
    }

    @Override
    public String reporterPhoneOf(Connection c, long repairOrderId) throws SQLException {
        String sql = "SELECT u.phone FROM repair_orders o JOIN users u ON u.id = o.reporter_id"
                + " WHERE o.id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, repairOrderId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getString(1) : null;
            }
        }
    }

    private static final String PERMIT_SELECT =
            "SELECT o.id, o.room_id, b.building_code, r.room_no, o.category, o.status,"
            + " o.submitted_at, p.allow_enter, p.note, u.phone AS contact_phone";
    private static final String PERMIT_FROM =
            " FROM repair_orders o"
            + " JOIN dorm_rooms r ON r.id = o.room_id"
            + " JOIN dorm_buildings b ON b.id = r.building_id"
            + " LEFT JOIN dorm_repair_entry_permits p ON p.repair_order_id = o.id"
            + " JOIN users u ON u.id = o.reporter_id";

    @Override
    public DormPage<RepairEntryPermitDto> listRepairPermits(Connection c, long studentUserId,
                                                            DormPageQuery query)
            throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        params.add(Long.valueOf(studentUserId));
        StringBuilder where = new StringBuilder(" WHERE o.reporter_id = ?");
        String status = JdbcDormSupport.clean(q.getStatus());
        if (status != null) {
            where.append(" AND o.status = ?");
            params.add(status);
        }
        String countSql = "SELECT COUNT(*)" + PERMIT_FROM + where;
        String dataSql = PERMIT_SELECT + PERMIT_FROM + where
                + " ORDER BY o.submitted_at DESC, o.id DESC LIMIT ? OFFSET ?";
        return JdbcDormSupport.page(c, countSql, dataSql, params, q.getPage(), q.getPageSize(),
                new JdbcDormSupport.Reader<RepairEntryPermitDto>() {
                    @Override
                    public RepairEntryPermitDto read(ResultSet r) throws SQLException {
                        return permit(r);
                    }
                });
    }

    private static RepairEntryPermitDto permit(ResultSet r) throws SQLException {
        boolean allow = r.getBoolean("allow_enter");
        if (r.wasNull()) allow = false;
        return new RepairEntryPermitDto(r.getLong("id"), r.getLong("room_id"),
                r.getString("building_code"), r.getString("room_no"), r.getString("category"),
                r.getString("status"), JdbcDormSupport.localTimestamp(r, "submitted_at"),
                allow, r.getString("note"), r.getString("contact_phone"));
    }

    @Override
    public int activeResidentCount(Connection c, long roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM accommodation_records ar"
                + " JOIN dorm_beds bd ON bd.id = ar.bed_id"
                + " WHERE bd.room_id = ? AND ar.status = 'ACTIVE'";
        return count(c, sql, roomId, 1);
    }

    @Override
    public int roomReferenceCount(Connection c, long roomId) throws SQLException {
        // 房间一旦被任何业务数据引用过就不能删：外键会挡住，而且删掉等于抹掉历史。
        String sql = "SELECT"
                + " (SELECT COUNT(*) FROM accommodation_records ar"
                + "   JOIN dorm_beds bd ON bd.id = ar.bed_id WHERE bd.room_id = ?)"
                + "+(SELECT COUNT(*) FROM utility_bills WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM hygiene_inspections WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM repair_orders WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM dorm_meter_readings WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM dorm_hygiene_tasks WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM dorm_absence_warnings WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM dorm_visitor_registrations WHERE room_id = ?)";
        return count(c, sql, roomId, 8);
    }

    private static int count(Connection c, String sql, long roomId, int placeholders)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 1; i <= placeholders; i++) s.setLong(i, roomId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        }
    }

    @Override
    public void deleteRoom(Connection c, long roomId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("DELETE FROM dorm_beds WHERE room_id = ?")) {
            s.setLong(1, roomId);
            s.executeUpdate();
        }
        try (PreparedStatement s = c.prepareStatement("DELETE FROM dorm_rooms WHERE id = ?")) {
            s.setLong(1, roomId);
            if (s.executeUpdate() == 0) {
                throw new DormRepositoryException(DormExtCommands.ROOM_NOT_FOUND, "房间不存在");
            }
        }
    }

    // ================= 卫生分项与检查任务 =================

    private static final String TASK_COLUMNS =
            "t.id, t.room_id, b.building_code, r.room_no, t.task_type, t.plan_date, t.status,"
            + " t.inspection_id, t.source_inspection_id";
    private static final String TASK_FROM =
            " FROM dorm_hygiene_tasks t"
            + " JOIN dorm_rooms r ON r.id = t.room_id"
            + " JOIN dorm_buildings b ON b.id = r.building_id";

    @Override
    public long createInspection(Connection c, long roomId, long inspectorId,
                                 LocalDateTime inspectedAt, BigDecimal totalScore, String result,
                                 String status, String issueDescription) throws SQLException {
        // 写的是 V1 建立的 hygiene_inspections：分项是扩展出来的明细，主记录仍然
        // 落在原表上，宿管端既有的「日常治理」页面因此能立刻看到这次检查。
        String sql = "INSERT INTO hygiene_inspections(room_id,inspector_id,inspected_at,score,"
                + "result,issue_description,status) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, roomId);
            s.setLong(2, inspectorId);
            s.setTimestamp(3, JdbcTemporal.timestamp(inspectedAt));
            s.setBigDecimal(4, totalScore);
            s.setString(5, result);
            s.setString(6, JdbcDormSupport.clean(issueDescription));
            s.setString(7, status);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("hygiene inspection id was not generated");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public void saveItemScores(Connection c, long inspectionId, List<HygieneItemScoreDto> items)
            throws SQLException {
        String sql = "INSERT INTO dorm_hygiene_item_scores(inspection_id,item_code,score,"
                + "deduct_reason) VALUES(?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (HygieneItemScoreDto item : items) {
                s.setLong(1, inspectionId);
                s.setString(2, item.getItemCode());
                s.setBigDecimal(3, item.getScore());
                s.setString(4, JdbcDormSupport.clean(item.getDeductReason()));
                s.addBatch();
            }
            s.executeBatch();
        }
    }

    @Override
    public HygieneDetailDto findInspectionDetail(Connection c, long inspectionId)
            throws SQLException {
        String sql = "SELECT h.id, h.room_id, b.building_code, r.room_no, h.inspector_id,"
                + " h.inspected_at, h.score, h.issue_description"
                + " FROM hygiene_inspections h"
                + " JOIN dorm_rooms r ON r.id = h.room_id"
                + " JOIN dorm_buildings b ON b.id = r.building_id"
                + " WHERE h.id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, inspectionId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                BigDecimal total = r.getBigDecimal("score");
                LocalDateTime inspectedAt = JdbcDormSupport.localTimestamp(r, "inspected_at");
                boolean rectify = DormHygieneRules.needRectify(total);
                return new HygieneDetailDto(r.getLong("id"), r.getLong("room_id"),
                        r.getString("building_code"), r.getString("room_no"),
                        r.getLong("inspector_id"), inspectedAt, total,
                        DormHygieneRules.level(total), rectify,
                        rectify ? DormHygieneRules.recheckDate(inspectedAt.toLocalDate()) : null,
                        r.getString("issue_description"), itemScores(c, inspectionId));
            }
        }
    }

    private static List<HygieneItemScoreDto> itemScores(Connection c, long inspectionId)
            throws SQLException {
        String sql = "SELECT item_code, score, deduct_reason FROM dorm_hygiene_item_scores"
                + " WHERE inspection_id = ? ORDER BY id";
        List<HygieneItemScoreDto> rows = new ArrayList<HygieneItemScoreDto>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, inspectionId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    rows.add(new HygieneItemScoreDto(r.getString("item_code"),
                            r.getBigDecimal("score"), r.getString("deduct_reason")));
                }
            }
        }
        return rows;
    }

    @Override
    public List<Long> roomsForWeeklyTask(Connection c, Long buildingId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id FROM dorm_rooms WHERE status <> 'CLOSED'");
        if (buildingId != null) sql.append(" AND building_id = ?");
        sql.append(" ORDER BY id");
        List<Long> rows = new ArrayList<Long>();
        try (PreparedStatement s = c.prepareStatement(sql.toString())) {
            if (buildingId != null) s.setLong(1, buildingId.longValue());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(Long.valueOf(r.getLong(1)));
            }
        }
        return rows;
    }

    @Override
    public boolean createTaskIfAbsent(Connection c, long roomId, String taskType,
                                      LocalDate planDate, Long sourceInspectionId)
            throws SQLException {
        // INSERT IGNORE 配合唯一键实现幂等：并发或重复调用都不会产生第二条任务，
        // 受影响行数为 0 即表示已经存在。
        String sql = "INSERT IGNORE INTO dorm_hygiene_tasks(room_id,task_type,plan_date,"
                + "source_inspection_id) VALUES(?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, roomId);
            s.setString(2, taskType);
            s.setDate(3, JdbcTemporal.date(planDate));
            if (sourceInspectionId == null) s.setNull(4, java.sql.Types.BIGINT);
            else s.setLong(4, sourceInspectionId.longValue());
            return s.executeUpdate() > 0;
        }
    }

    @Override
    public int markTasksDone(Connection c, long roomId, LocalDate onOrBefore, long inspectionId)
            throws SQLException {
        String sql = "UPDATE dorm_hygiene_tasks SET status = 'DONE', inspection_id = ?"
                + " WHERE room_id = ? AND status = 'PENDING' AND plan_date <= ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, inspectionId);
            s.setLong(2, roomId);
            s.setDate(3, JdbcTemporal.date(onOrBefore));
            return s.executeUpdate();
        }
    }

    @Override
    public DormPage<HygieneTaskDto> listTasks(Connection c, DormPageQuery query)
            throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q.getRoomId() != null) {
            where.append(" AND t.room_id = ?");
            params.add(q.getRoomId());
        }
        if (q.getBuildingId() != null) {
            where.append(" AND r.building_id = ?");
            params.add(q.getBuildingId());
        }
        String status = JdbcDormSupport.clean(q.getStatus());
        if (status != null) {
            where.append(" AND t.status = ?");
            params.add(status);
        }
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) {
            where.append(" AND (r.room_no LIKE ? OR b.building_code LIKE ? OR t.task_type LIKE ?)");
            String like = "%" + keyword + "%";
            for (int i = 0; i < 3; i++) params.add(like);
        }
        String countSql = "SELECT COUNT(*)" + TASK_FROM + where;
        String dataSql = "SELECT " + TASK_COLUMNS + TASK_FROM + where
                // 排序本身就是待办顺序：待检查排最前，其中复查优先于周检查
                // （复查意味着上次不合格），同组内按计划日期从早到晚。
                + " ORDER BY CASE t.status WHEN 'PENDING' THEN 0 WHEN 'SKIPPED' THEN 1 ELSE 2 END,"
                + " CASE t.task_type WHEN 'RECHECK' THEN 0 ELSE 1 END,"
                + " t.plan_date ASC, t.id ASC LIMIT ? OFFSET ?";
        return JdbcDormSupport.page(c, countSql, dataSql, params, q.getPage(), q.getPageSize(),
                new JdbcDormSupport.Reader<HygieneTaskDto>() {
                    @Override
                    public HygieneTaskDto read(ResultSet result) throws SQLException {
                        return task(result);
                    }
                });
    }

    private static HygieneTaskDto task(ResultSet r) throws SQLException {
        return new HygieneTaskDto(r.getLong("id"), r.getLong("room_id"),
                r.getString("building_code"), r.getString("room_no"), r.getString("task_type"),
                JdbcDormSupport.localDate(r, "plan_date"), r.getString("status"),
                JdbcDormSupport.longOrNull(r, "inspection_id"),
                JdbcDormSupport.longOrNull(r, "source_inspection_id"));
    }

    // ================= 外来人员登记 =================

    private static final String VISITOR_COLUMNS =
            "v.id, v.student_user_id, v.room_id, b.building_code, r.room_no, v.visitor_name,"
            + " v.visitor_id_card, v.visitor_phone, v.visit_reason, v.start_at, v.end_at,"
            + " v.submitted_at, v.audit_status, v.auditor_id, v.audited_at, v.audit_remark";
    private static final String VISITOR_FROM =
            " FROM dorm_visitor_registrations v"
            + " JOIN dorm_rooms r ON r.id = v.room_id"
            + " JOIN dorm_buildings b ON b.id = r.building_id";

    @Override
    public Long activeRoomOf(Connection c, long studentUserId) throws SQLException {
        String sql = "SELECT bd.room_id FROM accommodation_records ar"
                + " JOIN dorm_beds bd ON bd.id = ar.bed_id"
                + " WHERE ar.student_user_id = ? AND ar.status = 'ACTIVE'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, studentUserId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Long.valueOf(r.getLong(1)) : null;
            }
        }
    }

    @Override
    public VisitorRegistrationDto createVisitor(Connection c, long studentUserId, long roomId,
                                                VisitorRegistrationRequest request)
            throws SQLException {
        String sql = "INSERT INTO dorm_visitor_registrations(student_user_id,room_id,visitor_name,"
                + "visitor_id_card,visitor_phone,visit_reason,start_at,end_at)"
                + " VALUES(?,?,?,?,?,?,?,?)";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, studentUserId);
            s.setLong(2, roomId);
            s.setString(3, request.getVisitorName().trim());
            s.setString(4, request.getVisitorIdCard().trim());
            s.setString(5, JdbcDormSupport.clean(request.getVisitorPhone()));
            s.setString(6, request.getVisitReason().trim());
            s.setTimestamp(7, JdbcTemporal.timestamp(request.getStartAt()));
            s.setTimestamp(8, JdbcTemporal.timestamp(request.getEndAt()));
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("visitor registration id was not generated");
                id = keys.getLong(1);
            }
        }
        return findVisitor(c, id);
    }

    @Override
    public DormPage<VisitorRegistrationDto> listVisitors(Connection c, DormPageQuery query,
                                                         Long studentUserId) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (studentUserId != null) {
            where.append(" AND v.student_user_id = ?");
            params.add(studentUserId);
        }
        if (q.getRoomId() != null) {
            where.append(" AND v.room_id = ?");
            params.add(q.getRoomId());
        }
        String status = JdbcDormSupport.clean(q.getStatus());
        if (status != null) {
            where.append(" AND v.audit_status = ?");
            params.add(status);
        }
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) {
            where.append(" AND (v.visitor_name LIKE ? OR v.visit_reason LIKE ?"
                    + " OR r.room_no LIKE ? OR b.building_code LIKE ?)");
            String like = "%" + keyword + "%";
            for (int i = 0; i < 4; i++) params.add(like);
        }
        String countSql = "SELECT COUNT(*)" + VISITOR_FROM + where;
        String dataSql = "SELECT " + VISITOR_COLUMNS + VISITOR_FROM + where
                + " ORDER BY v.submitted_at DESC, v.id DESC LIMIT ? OFFSET ?";
        return JdbcDormSupport.page(c, countSql, dataSql, params, q.getPage(), q.getPageSize(),
                new JdbcDormSupport.Reader<VisitorRegistrationDto>() {
                    @Override
                    public VisitorRegistrationDto read(ResultSet result) throws SQLException {
                        return visitor(result);
                    }
                });
    }

    @Override
    public VisitorRegistrationDto findVisitor(Connection c, long registrationId)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT " + VISITOR_COLUMNS + VISITOR_FROM + " WHERE v.id = ?")) {
            s.setLong(1, registrationId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? visitor(r) : null;
            }
        }
    }

    @Override
    public VisitorRegistrationDto updateVisitorStatus(Connection c, long registrationId,
                                                      String status, Long auditorId,
                                                      LocalDateTime auditedAt, String remark)
            throws SQLException {
        String sql = "UPDATE dorm_visitor_registrations SET audit_status = ?, auditor_id = ?,"
                + " audited_at = ?, audit_remark = ? WHERE id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, status);
            if (auditorId == null) s.setNull(2, java.sql.Types.BIGINT);
            else s.setLong(2, auditorId.longValue());
            s.setTimestamp(3, JdbcTemporal.timestamp(auditedAt));
            s.setString(4, JdbcDormSupport.clean(remark));
            s.setLong(5, registrationId);
            if (s.executeUpdate() == 0) {
                throw new DormRepositoryException(DormExtCommands.VISITOR_NOT_FOUND, "来访登记不存在");
            }
        }
        return findVisitor(c, registrationId);
    }

    /** 证件号在这里就转成掩码：完整值不离开数据库。 */
    private static VisitorRegistrationDto visitor(ResultSet r) throws SQLException {
        return new VisitorRegistrationDto(r.getLong("id"), r.getLong("student_user_id"),
                r.getLong("room_id"), r.getString("building_code"), r.getString("room_no"),
                r.getString("visitor_name"),
                VisitorRegistrationDto.mask(r.getString("visitor_id_card")),
                r.getString("visitor_phone"), r.getString("visit_reason"),
                JdbcDormSupport.localTimestamp(r, "start_at"),
                JdbcDormSupport.localTimestamp(r, "end_at"),
                JdbcDormSupport.localTimestamp(r, "submitted_at"),
                r.getString("audit_status"), JdbcDormSupport.longOrNull(r, "auditor_id"),
                JdbcDormSupport.localTimestamp(r, "audited_at"), r.getString("audit_remark"));
    }

    // ================= 未归预警 =================

    private static final String WARNING_COLUMNS =
            "w.id, w.student_user_id, w.room_id, b.building_code, r.room_no, w.scan_date,"
            + " w.last_leave_at, w.absence_days, w.warning_level, w.handle_status,"
            + " w.notified_teacher_id, w.notified_at, w.note";
    private static final String WARNING_FROM =
            " FROM dorm_absence_warnings w"
            + " JOIN dorm_rooms r ON r.id = w.room_id"
            + " JOIN dorm_buildings b ON b.id = r.building_id";

    @Override
    public List<ResidentAbsenceSnapshot> residentsForScan(Connection c) throws SQLException {
        // 两个相关子查询各自命中 idx_access_records_student_time，比先取全部流水再在
        // 内存里分组便宜得多，也避免把整张门禁表拉进服务端。
        String sql = "SELECT ar.student_user_id, bd.room_id,"
                + " (SELECT MAX(x.occurred_at) FROM access_records x"
                + "   WHERE x.student_user_id = ar.student_user_id AND x.record_type = 'EXIT')"
                + "  AS last_exit_at,"
                + " (SELECT MAX(x.occurred_at) FROM access_records x"
                + "   WHERE x.student_user_id = ar.student_user_id AND x.record_type = 'ENTRY')"
                + "  AS last_entry_at"
                + " FROM accommodation_records ar"
                + " JOIN dorm_beds bd ON bd.id = ar.bed_id"
                + " WHERE ar.status = 'ACTIVE'"
                + " ORDER BY ar.student_user_id";
        List<ResidentAbsenceSnapshot> rows = new ArrayList<ResidentAbsenceSnapshot>();
        try (PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                rows.add(new ResidentAbsenceSnapshot(r.getLong("student_user_id"),
                        r.getLong("room_id"),
                        JdbcDormSupport.localTimestamp(r, "last_exit_at"),
                        JdbcDormSupport.localTimestamp(r, "last_entry_at")));
            }
        }
        return rows;
    }

    @Override
    public boolean hasApprovedLeave(Connection c, long studentUserId, LocalDate date)
            throws SQLException {
        String sql = "SELECT 1 FROM leave_requests"
                + " WHERE student_user_id = ? AND status = 'APPROVED'"
                + " AND start_at <= ? AND end_at >= ? LIMIT 1";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, studentUserId);
            s.setTimestamp(2, JdbcTemporal.timestamp(date.atTime(23, 59, 59)));
            s.setTimestamp(3, JdbcTemporal.timestamp(date.atStartOfDay()));
            try (ResultSet r = s.executeQuery()) {
                return r.next();
            }
        }
    }

    @Override
    public AbsenceWarningDto saveWarning(Connection c, long studentUserId, long roomId,
                                         LocalDate scanDate, LocalDateTime lastLeaveAt,
                                         int absenceDays, String warningLevel)
            throws SQLException {
        String sql = "INSERT INTO dorm_absence_warnings(student_user_id,room_id,scan_date,"
                + "last_leave_at,absence_days,warning_level) VALUES(?,?,?,?,?,?) AS incoming"
                + " ON DUPLICATE KEY UPDATE room_id = incoming.room_id,"
                + " last_leave_at = incoming.last_leave_at,"
                + " absence_days = incoming.absence_days,"
                + " warning_level = incoming.warning_level";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, studentUserId);
            s.setLong(2, roomId);
            s.setDate(3, JdbcTemporal.date(scanDate));
            s.setTimestamp(4, JdbcTemporal.timestamp(lastLeaveAt));
            s.setInt(5, absenceDays);
            s.setString(6, warningLevel);
            s.executeUpdate();
        }
        String find = "SELECT " + WARNING_COLUMNS + WARNING_FROM
                + " WHERE w.student_user_id = ? AND w.scan_date = ?";
        try (PreparedStatement s = c.prepareStatement(find)) {
            s.setLong(1, studentUserId);
            s.setDate(2, JdbcTemporal.date(scanDate));
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) {
                    throw new DormRepositoryException(DormExtCommands.WARNING_NOT_FOUND,
                            "预警写入后无法读回");
                }
                return warning(r);
            }
        }
    }

    @Override
    public DormPage<AbsenceWarningDto> listWarnings(Connection c, DormPageQuery query)
            throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q.getRoomId() != null) {
            where.append(" AND w.room_id = ?");
            params.add(q.getRoomId());
        }
        if (q.getBuildingId() != null) {
            where.append(" AND r.building_id = ?");
            params.add(q.getBuildingId());
        }
        String status = JdbcDormSupport.clean(q.getStatus());
        if (status != null) {
            where.append(" AND w.handle_status = ?");
            params.add(status);
        }
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) {
            where.append(" AND (r.room_no LIKE ? OR b.building_code LIKE ?"
                    + " OR w.warning_level LIKE ? OR CAST(w.student_user_id AS CHAR) LIKE ?)");
            String like = "%" + keyword + "%";
            for (int i = 0; i < 4; i++) params.add(like);
        }
        String countSql = "SELECT COUNT(*)" + WARNING_FROM + where;
        String dataSql = "SELECT " + WARNING_COLUMNS + WARNING_FROM + where
                + " ORDER BY w.scan_date DESC, w.absence_days DESC, w.id DESC LIMIT ? OFFSET ?";
        return JdbcDormSupport.page(c, countSql, dataSql, params, q.getPage(), q.getPageSize(),
                new JdbcDormSupport.Reader<AbsenceWarningDto>() {
                    @Override
                    public AbsenceWarningDto read(ResultSet result) throws SQLException {
                        return warning(result);
                    }
                });
    }

    @Override
    public AbsenceWarningDto findWarning(Connection c, long warningId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT " + WARNING_COLUMNS + WARNING_FROM + " WHERE w.id = ?")) {
            s.setLong(1, warningId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? warning(r) : null;
            }
        }
    }

    @Override
    public AbsenceWarningDto updateWarningStatus(Connection c, long warningId, String status,
                                                 Long teacherUserId, LocalDateTime notifiedAt,
                                                 String note) throws SQLException {
        String sql = "UPDATE dorm_absence_warnings SET handle_status = ?,"
                + " notified_teacher_id = ?, notified_at = ?, note = ? WHERE id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, status);
            if (teacherUserId == null) s.setNull(2, java.sql.Types.BIGINT);
            else s.setLong(2, teacherUserId.longValue());
            s.setTimestamp(3, JdbcTemporal.timestamp(notifiedAt));
            s.setString(4, JdbcDormSupport.clean(note));
            s.setLong(5, warningId);
            if (s.executeUpdate() == 0) {
                throw new DormRepositoryException(DormExtCommands.WARNING_NOT_FOUND, "预警不存在");
            }
        }
        return findWarning(c, warningId);
    }

    @Override
    public WarningConfigDto loadWarningConfig(Connection c) throws SQLException {
        String sql = "SELECT warn_days, notify_days, exempt_on_leave, updated_at"
                + " FROM dorm_warning_configs WHERE id = 1";
        try (PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            if (!r.next()) {
                throw new DormRepositoryException(DormExtCommands.CONFIG_INVALID,
                        "未归预警阈值未初始化，请先执行 V3 迁移");
            }
            return new WarningConfigDto(r.getInt("warn_days"), r.getInt("notify_days"),
                    r.getBoolean("exempt_on_leave"),
                    JdbcDormSupport.localTimestamp(r, "updated_at"));
        }
    }

    @Override
    public WarningConfigDto saveWarningConfig(Connection c, WarningConfigRequest request,
                                              long actorUserId) throws SQLException {
        String sql = "UPDATE dorm_warning_configs SET warn_days = ?, notify_days = ?,"
                + " exempt_on_leave = ?, updated_by = ? WHERE id = 1";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, request.getWarnDays());
            s.setInt(2, request.getNotifyDays());
            s.setBoolean(3, request.isExemptOnLeave());
            s.setLong(4, actorUserId);
            s.executeUpdate();
        }
        return loadWarningConfig(c);
    }

    private static AbsenceWarningDto warning(ResultSet r) throws SQLException {
        return new AbsenceWarningDto(r.getLong("id"), r.getLong("student_user_id"),
                r.getLong("room_id"), r.getString("building_code"), r.getString("room_no"),
                JdbcDormSupport.localDate(r, "scan_date"),
                JdbcDormSupport.localTimestamp(r, "last_leave_at"),
                r.getInt("absence_days"), r.getString("warning_level"),
                r.getString("handle_status"), JdbcDormSupport.longOrNull(r, "notified_teacher_id"),
                JdbcDormSupport.localTimestamp(r, "notified_at"), r.getString("note"));
    }

    private static MeterReadingDto meterReading(ResultSet r) throws SQLException {
        return new MeterReadingDto(r.getLong("id"), r.getLong("room_id"),
                r.getString("building_code"), r.getString("room_no"),
                JdbcDormSupport.localDate(r, "period_start"),
                JdbcDormSupport.localDate(r, "period_end"),
                r.getBigDecimal("electricity_units"), r.getBigDecimal("water_units"),
                r.getBigDecimal("electricity_price"), r.getBigDecimal("water_price"),
                r.getLong("recorded_by"), JdbcDormSupport.localTimestamp(r, "recorded_at"),
                JdbcDormSupport.longOrNull(r, "bill_id"));
    }

    // ---- 公告类型、范围与置顶 ----

    private static final String NOTICE_COLUMNS =
            "a.id, a.title, a.content, a.status, a.publish_at, a.expire_at, a.publisher_id,"
            + " COALESCE(x.notice_type, 'GENERAL') AS notice_type,"
            + " COALESCE(x.scope_type, 'ALL') AS scope_type,"
            + " x.scope_building_id, x.scope_room_id, b.building_code, r.room_no,"
            + " COALESCE(x.pinned, 0) AS pinned, x.pinned_at";
    // 楼栋既可能直接挂在扩展记录上（范围=楼栋），也可能要从房间反推（范围=房间），
    // 所以 building 的连接键用 COALESCE 兜住两种情况。
    private static final String NOTICE_FROM =
            " FROM announcements a"
            + " LEFT JOIN dorm_notice_extras x ON x.announcement_id = a.id"
            + " LEFT JOIN dorm_rooms r ON r.id = x.scope_room_id"
            + " LEFT JOIN dorm_buildings b ON b.id = COALESCE(x.scope_building_id, r.building_id)";
    private static final String NOTICE_ORDER =
            " ORDER BY COALESCE(x.pinned, 0) DESC, x.pinned_at DESC,"
            + " COALESCE(a.publish_at, a.created_at) DESC, a.id DESC";

    @Override
    public DormPage<NoticeExtraDto> listNotices(Connection c, DormPageQuery query,
                                                boolean manageView, Long viewerRoomId)
            throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE a.module_code = 'DORM'");
        if (!manageView) {
            where.append(" AND a.status = 'PUBLISHED'")
                    .append(" AND (a.publish_at IS NULL OR a.publish_at <= CURRENT_TIMESTAMP(3))")
                    .append(" AND (a.expire_at IS NULL OR a.expire_at > CURRENT_TIMESTAMP(3))");
            // 没有扩展记录的老公告 scope_type 为 NULL，COALESCE 成 ALL，对所有人可见。
            where.append(" AND (COALESCE(x.scope_type, 'ALL') = 'ALL'");
            if (viewerRoomId != null) {
                where.append(" OR (x.scope_type = 'ROOM' AND x.scope_room_id = ?)")
                        .append(" OR (x.scope_type = 'BUILDING' AND x.scope_building_id =")
                        .append(" (SELECT vr.building_id FROM dorm_rooms vr WHERE vr.id = ?))");
                params.add(viewerRoomId);
                params.add(viewerRoomId);
            }
            where.append(')');
        }
        String status = JdbcDormSupport.clean(q.getStatus());
        if (status != null) {
            // status 在管理视图下过滤公告状态，在学生视图下已被上面的条件收窄，
            // 这里复用成公告类型过滤，客户端的类型下拉走的就是它。
            if (manageView) {
                where.append(" AND a.status = ?");
            } else {
                where.append(" AND COALESCE(x.notice_type, 'GENERAL') = ?");
            }
            params.add(status);
        }
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) {
            where.append(" AND (a.title LIKE ? OR a.content LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
        }
        String countSql = "SELECT COUNT(*)" + NOTICE_FROM + where;
        String dataSql = "SELECT " + NOTICE_COLUMNS + NOTICE_FROM + where + NOTICE_ORDER
                + " LIMIT ? OFFSET ?";
        return JdbcDormSupport.page(c, countSql, dataSql, params, q.getPage(), q.getPageSize(),
                new JdbcDormSupport.Reader<NoticeExtraDto>() {
                    @Override
                    public NoticeExtraDto read(ResultSet r) throws SQLException {
                        return notice(r);
                    }
                });
    }

    @Override
    public NoticeExtraDto findNotice(Connection c, long announcementId) throws SQLException {
        String sql = "SELECT " + NOTICE_COLUMNS + NOTICE_FROM
                + " WHERE a.module_code = 'DORM' AND a.id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, announcementId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? notice(r) : null;
            }
        }
    }

    @Override
    public NoticeExtraDto saveNoticeExtra(Connection c, NoticeExtraRequest request,
                                          long actorUserId) throws SQLException {
        // 置顶时刻只在「从未置顶变为置顶」时刷新，重复保存同一条置顶公告不会把它
        // 顶到其它置顶公告前面；取消置顶则清空，便于下次重新排序。
        String sql = "INSERT INTO dorm_notice_extras(announcement_id,notice_type,scope_type,"
                + "scope_building_id,scope_room_id,pinned,pinned_at,updated_by)"
                + " VALUES(?,?,?,?,?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE notice_type = VALUES(notice_type),"
                + " scope_type = VALUES(scope_type),"
                + " scope_building_id = VALUES(scope_building_id),"
                + " scope_room_id = VALUES(scope_room_id),"
                + " pinned_at = CASE WHEN VALUES(pinned) = 0 THEN NULL"
                + " WHEN pinned = 1 THEN pinned_at ELSE VALUES(pinned_at) END,"
                + " pinned = VALUES(pinned), updated_by = VALUES(updated_by)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, request.getAnnouncementId());
            s.setString(2, request.getNoticeType());
            s.setString(3, request.getScopeType());
            setNullableLong(s, 4, request.getScopeBuildingId());
            setNullableLong(s, 5, request.getScopeRoomId());
            s.setBoolean(6, request.isPinned());
            if (request.isPinned()) {
                s.setTimestamp(7, JdbcTemporal.timestamp(LocalDateTime.now()));
            } else {
                s.setNull(7, java.sql.Types.TIMESTAMP);
            }
            s.setLong(8, actorUserId);
            s.executeUpdate();
        }
        return findNotice(c, request.getAnnouncementId());
    }

    private static void setNullableLong(PreparedStatement s, int index, Long value)
            throws SQLException {
        if (value == null) {
            s.setNull(index, java.sql.Types.BIGINT);
        } else {
            s.setLong(index, value.longValue());
        }
    }

    private static NoticeExtraDto notice(ResultSet r) throws SQLException {
        return new NoticeExtraDto(r.getLong("id"), r.getString("title"), r.getString("content"),
                r.getString("status"), JdbcDormSupport.localTimestamp(r, "publish_at"),
                JdbcDormSupport.localTimestamp(r, "expire_at"), r.getLong("publisher_id"),
                r.getString("notice_type"), r.getString("scope_type"),
                JdbcDormSupport.longOrNull(r, "scope_building_id"),
                JdbcDormSupport.longOrNull(r, "scope_room_id"),
                r.getString("building_code"), r.getString("room_no"),
                r.getBoolean("pinned"), JdbcDormSupport.localTimestamp(r, "pinned_at"));
    }

    // ---- 定时任务专用 ----

    @Override
    public int expireDormAnnouncements(Connection c, LocalDateTime now) throws SQLException {
        // 条件里写死 module_code = 'DORM'，别的模块的公告永远不会被本任务碰到；
        // EXPIRED 是 ck_announcements_status 已允许的取值，不需要改表。
        String sql = "UPDATE announcements SET status = 'EXPIRED'"
                + " WHERE module_code = 'DORM' AND status = 'PUBLISHED'"
                + " AND expire_at IS NOT NULL AND expire_at < ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setTimestamp(1, JdbcTemporal.timestamp(now == null ? LocalDateTime.now() : now));
            return s.executeUpdate();
        }
    }

    @Override
    public List<AbsenceWarningDto> pendingSevereWarnings(Connection c, LocalDate onOrBefore)
            throws SQLException {
        String sql = "SELECT " + WARNING_COLUMNS + WARNING_FROM
                + " WHERE w.warning_level = ? AND w.handle_status = ? AND w.scan_date <= ?"
                + " ORDER BY w.absence_days DESC, w.id ASC";
        List<AbsenceWarningDto> rows = new ArrayList<AbsenceWarningDto>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, AbsenceWarningDto.LEVEL_SEVERE);
            s.setString(2, AbsenceWarningDto.STATUS_PENDING);
            s.setDate(3, JdbcTemporal.date(onOrBefore == null ? LocalDate.now() : onOrBefore));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    rows.add(warning(r));
                }
            }
        }
        return rows;
    }
}
