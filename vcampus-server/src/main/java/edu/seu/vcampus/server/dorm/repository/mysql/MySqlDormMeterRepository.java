package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** MySQL persistence for meter readings and utility-bill allocation primitives. */
final class MySqlDormMeterRepository {
    private static final String COLUMNS = "m.id, m.room_id, b.building_code, r.room_no, m.period_start, m.period_end,"
            + " m.electricity_units, m.water_units, m.electricity_price, m.water_price, m.recorded_by, m.recorded_at, m.bill_id";
    private static final String FROM = " FROM dorm_meter_readings m JOIN dorm_rooms r ON r.id = m.room_id"
            + " JOIN dorm_buildings b ON b.id = r.building_id";

    DormPage<MeterReadingDto> list(Connection c, DormPageQuery query) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q.getRoomId() != null) { where.append(" AND m.room_id = ?"); params.add(q.getRoomId()); }
        if (q.getBuildingId() != null) { where.append(" AND r.building_id = ?"); params.add(q.getBuildingId()); }
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) {
            where.append(" AND (r.room_no LIKE ? OR b.building_code LIKE ? OR b.building_name LIKE ?)");
            String like = "%" + keyword + "%"; params.add(like); params.add(like); params.add(like);
        }
        String countSql = "SELECT COUNT(*)" + FROM + where;
        String dataSql = "SELECT " + COLUMNS + FROM + where + " ORDER BY m.period_start DESC, m.id DESC LIMIT ? OFFSET ?";
        return JdbcDormSupport.page(c, countSql, dataSql, params, q.getPage(), q.getPageSize(),
                new JdbcDormSupport.Reader<MeterReadingDto>() {
                    @Override public MeterReadingDto read(ResultSet r) throws SQLException { return MySqlDormExtSupport.meter(r); }
                });
    }

    MeterReadingDto save(Connection c, MeterReadingRequest request, long actor) throws SQLException {
        ensureRoom(c, request.getRoomId());
        long id = findId(c, request);
        if (id > 0L) { update(c, id, request, actor); return require(c, id); }
        return require(c, insert(c, request, actor));
    }

    List<MeterReadingDto> pending(Connection c, Long room, LocalDate start, LocalDate end) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(FROM)
                .append(" WHERE m.bill_id IS NULL AND m.period_start = ? AND m.period_end = ?");
        if (room != null) sql.append(" AND m.room_id = ?"); sql.append(" ORDER BY m.room_id");
        List<MeterReadingDto> rows = new ArrayList<MeterReadingDto>();
        try (PreparedStatement s = c.prepareStatement(sql.toString())) {
            s.setDate(1, JdbcTemporal.date(start)); s.setDate(2, JdbcTemporal.date(end));
            if (room != null) s.setLong(3, room.longValue());
            try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(MySqlDormExtSupport.meter(r)); }
        }
        return rows;
    }

    List<Long> activeResidents(Connection c, long room) throws SQLException {
        String sql = "SELECT ar.student_user_id FROM accommodation_records ar JOIN dorm_beds bd ON bd.id = ar.bed_id"
                + " WHERE bd.room_id = ? AND ar.status = 'ACTIVE' ORDER BY ar.student_user_id";
        List<Long> rows = new ArrayList<Long>();
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, room); try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(Long.valueOf(r.getLong(1))); } }
        return rows;
    }

    boolean billExists(Connection c, long room, LocalDate start, LocalDate end) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM utility_bills WHERE room_id = ? AND period_start = ? AND period_end = ?")) {
            s.setLong(1, room); s.setDate(2, JdbcTemporal.date(start)); s.setDate(3, JdbcTemporal.date(end));
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    long createBill(Connection c, MeterReadingDto reading, BigDecimal amount, LocalDateTime due, long actor) throws SQLException {
        String sql = "INSERT INTO utility_bills(room_id,period_start,period_end,electricity_units,water_units,total_amount,due_at,status,created_by) VALUES(?,?,?,?,?,?,?,'UNPAID',?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, reading.getRoomId()); s.setDate(2, JdbcTemporal.date(reading.getPeriodStart())); s.setDate(3, JdbcTemporal.date(reading.getPeriodEnd()));
            s.setBigDecimal(4, reading.getElectricityUnits()); s.setBigDecimal(5, reading.getWaterUnits()); s.setBigDecimal(6, amount);
            s.setTimestamp(7, JdbcTemporal.timestamp(due)); s.setLong(8, actor); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("utility bill id was not generated"); return keys.getLong(1); }
        }
    }

    void createAllocation(Connection c, long bill, long student, BigDecimal amount) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO utility_allocations(bill_id,student_user_id,amount,status) VALUES(?,?,?,'UNPAID')")) {
            s.setLong(1, bill); s.setLong(2, student); s.setBigDecimal(3, amount); s.executeUpdate();
        }
    }

    void link(Connection c, long reading, long bill) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE dorm_meter_readings SET bill_id = ? WHERE id = ? AND bill_id IS NULL")) {
            s.setLong(1, bill); s.setLong(2, reading);
            if (s.executeUpdate() == 0) throw new DormRepositoryException(DormExtCommands.METER_LOCKED, "抄表读数已被其他账单占用");
        }
    }

    private static void ensureRoom(Connection c, long room) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM dorm_rooms WHERE id = ?")) { s.setLong(1, room); try (ResultSet r = s.executeQuery()) {
            if (!r.next()) throw new DormRepositoryException(DormExtCommands.ROOM_NOT_FOUND, "房间不存在");
        } }
    }

    private static long findId(Connection c, MeterReadingRequest request) throws SQLException {
        String sql = "SELECT id, bill_id FROM dorm_meter_readings WHERE room_id = ? AND period_start = ? AND period_end = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, request.getRoomId()); s.setDate(2, JdbcTemporal.date(request.getPeriodStart())); s.setDate(3, JdbcTemporal.date(request.getPeriodEnd())); try (ResultSet r = s.executeQuery()) {
            if (!r.next()) return 0L; long id = r.getLong("id"); r.getLong("bill_id");
            if (!r.wasNull()) throw new DormRepositoryException(DormExtCommands.METER_LOCKED, "该账期已生成账单，读数不可修改"); return id;
        } }
    }

    private static long insert(Connection c, MeterReadingRequest request, long actor) throws SQLException {
        String sql = "INSERT INTO dorm_meter_readings(room_id,period_start,period_end,electricity_units,water_units,electricity_price,water_price,recorded_by) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { s.setLong(1, request.getRoomId()); s.setDate(2, JdbcTemporal.date(request.getPeriodStart())); s.setDate(3, JdbcTemporal.date(request.getPeriodEnd())); bind(s, request, 4); s.setLong(8, actor); s.executeUpdate(); try (ResultSet k = s.getGeneratedKeys()) { if (!k.next()) throw new SQLException("meter reading id was not generated"); return k.getLong(1); } }
    }

    private static void update(Connection c, long id, MeterReadingRequest request, long actor) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE dorm_meter_readings SET electricity_units = ?, water_units = ?, electricity_price = ?, water_price = ?, recorded_by = ? WHERE id = ? AND bill_id IS NULL")) {
            bind(s, request, 1); s.setLong(5, actor); s.setLong(6, id); if (s.executeUpdate() == 0) throw new DormRepositoryException(DormExtCommands.METER_LOCKED, "该账期已生成账单，读数不可修改");
        }
    }

    private static void bind(PreparedStatement s, MeterReadingRequest r, int from) throws SQLException {
        s.setBigDecimal(from, r.getElectricityUnits()); s.setBigDecimal(from + 1, r.getWaterUnits()); s.setBigDecimal(from + 2, r.getElectricityPrice()); s.setBigDecimal(from + 3, r.getWaterPrice());
    }

    private static MeterReadingDto require(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE m.id = ?")) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) {
            if (!r.next()) throw new DormRepositoryException(DormExtCommands.METER_NOT_FOUND, "抄表读数不存在"); return MySqlDormExtSupport.meter(r);
        } }
    }
}
