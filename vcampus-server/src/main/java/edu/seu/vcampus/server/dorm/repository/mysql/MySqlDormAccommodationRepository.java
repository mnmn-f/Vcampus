package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormAccommodationRepository;
import edu.seu.vcampus.server.dorm.repository.DormFacilityRepository;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.threeten.bp.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** MySQL 住宿记录仓储；床位与申请变更要求调用方处于事务中。 */
public final class MySqlDormAccommodationRepository implements DormAccommodationRepository {
    private static final String RECORD_COLUMNS = "ar.id,ar.student_user_id,ar.bed_id,dr.id room_id,"
            + "db.id building_id,db.building_code,db.building_name,dr.room_no,b.bed_no,"
            + "ar.start_date,ar.end_date,ar.status";
    private static final String RECORD_FROM = " FROM accommodation_records ar JOIN dorm_beds b ON b.id=ar.bed_id"
            + " JOIN dorm_rooms dr ON dr.id=b.room_id JOIN dorm_buildings db ON db.id=dr.building_id";
    private final DormFacilityRepository facilities;

    public MySqlDormAccommodationRepository() { this(new MySqlDormFacilityRepository()); }
    public MySqlDormAccommodationRepository(DormFacilityRepository facilities) { this.facilities = facilities; }

    @Override
    public AccommodationDto findCurrent(Connection c, long student, boolean lock) throws SQLException {
        return findOne(c, "ar.student_user_id=? AND ar.status='ACTIVE'", student, lock);
    }

    @Override
    public AccommodationDto findById(Connection c, long id, boolean lock) throws SQLException {
        return findOne(c, "ar.id=?", id, lock);
    }

    @Override
    public AccommodationDto assign(Connection c, long student, long bed, LocalDate date, long actor)
            throws SQLException {
        MySqlDormAccommodationOps.requireStudent(c, student);
        if (findCurrent(c, student, true) != null) {
            throw new DormRepositoryException(DormCommands.ALREADY_ACCOMMODATED, "学生已有有效住宿");
        }
        MySqlDormAccommodationOps.requireAvailable(c, facilities, bed);
        MySqlDormAccommodationOps.insertRecord(c, student, bed, date == null ? LocalDate.now() : date, actor);
        MySqlDormAccommodationOps.occupy(c, bed);
        return findCurrent(c, student, false);
    }

    @Override
    public AccommodationDto transfer(Connection c, long student, long record, long target,
                                     LocalDate date, long actor) throws SQLException {
        MySqlDormAccommodationOps.requireStudent(c, student);
        AccommodationDto current = findCurrent(c, student, true);
        if (current == null || current.getId() != record) {
            throw new DormRepositoryException(DormCommands.ACCOMMODATION_NOT_FOUND, "当前住宿记录不存在");
        }
        MySqlDormAccommodationOps.requireAvailable(c, facilities, target);
        LocalDate effective = date == null ? LocalDate.now() : date;
        MySqlDormAccommodationOps.endRecord(c, record, effective);
        MySqlDormAccommodationOps.release(c, current.getBedId());
        MySqlDormAccommodationOps.insertRecord(c, student, target, effective, actor);
        MySqlDormAccommodationOps.occupy(c, target);
        return findCurrent(c, student, false);
    }

    @Override
    public AccommodationDto checkout(Connection c, long student, long record, LocalDate date, long actor)
            throws SQLException {
        MySqlDormAccommodationOps.requireStudent(c, student);
        AccommodationDto current = findCurrent(c, student, true);
        if (current == null || current.getId() != record) {
            throw new DormRepositoryException(DormCommands.ACCOMMODATION_NOT_FOUND, "当前住宿记录不存在");
        }
        MySqlDormAccommodationOps.endRecord(c, record, date == null ? LocalDate.now() : date);
        MySqlDormAccommodationOps.release(c, current.getBedId());
        return findById(c, record, false);
    }

    @Override
    public AccommodationRequestDto submitRequest(Connection c, long student, String type,
                                                 Long current, Long target, String reason)
            throws SQLException {
        MySqlDormAccommodationOps.requireStudent(c, student);
        if (type == null || (!"CHECK_IN".equals(type) && !"TRANSFER".equals(type)
                && !"CHECK_OUT".equals(type))) {
            throw new DormRepositoryException(DormCommands.INVALID_INPUT, "申请类型不正确");
        }
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM accommodation_requests"
                + " WHERE student_user_id=? AND status='PENDING' LIMIT 1")) {
            s.setLong(1, student);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) throw new DormRepositoryException(DormCommands.REQUEST_DUPLICATE, "已有待审批住宿申请");
            }
        }
        AccommodationDto existing = findCurrent(c, student, false);
        if ("CHECK_IN".equals(type) && existing != null) {
            throw new DormRepositoryException(DormCommands.ALREADY_ACCOMMODATED, "学生已有有效住宿");
        }
        if (("TRANSFER".equals(type) || "CHECK_OUT".equals(type))
                && (existing == null || current == null || existing.getId() != current.longValue())) {
            throw new DormRepositoryException(DormCommands.ACCOMMODATION_NOT_FOUND, "当前住宿记录不存在");
        }
        if ("CHECK_IN".equals(type) || "TRANSFER".equals(type)) {
            if (target == null) throw new DormRepositoryException(DormCommands.INVALID_INPUT, "目标床位不能为空");
            MySqlDormAccommodationOps.requireAvailable(c, facilities, target.longValue());
        } else if (target != null) {
            throw new DormRepositoryException(DormCommands.INVALID_INPUT, "退宿申请不能带目标床位");
        }
        String sql = "INSERT INTO accommodation_requests(student_user_id,request_type,current_record_id,"
                + "requested_bed_id,reason,status) VALUES(?,?,?,?,?,'PENDING')";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, student); s.setString(2, type); setLong(s, 3, current); setLong(s, 4, target);
            s.setString(5, reason); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("request id was not generated");
                id = keys.getLong(1);
            }
        }
        return lockRequest(c, id);
    }

    @Override
    public DormPage<AccommodationRequestDto> listRequests(Connection c, Long student,
                                                           DormPageQuery q) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = " WHERE 1=1";
        if (student != null) { where += " AND student_user_id=?"; p.add(student); }
        if (JdbcDormSupport.clean(query.getStatus()) != null) { where += " AND status=?"; p.add(query.getStatus().trim()); }
        return JdbcDormSupport.page(c, "SELECT COUNT(*) FROM accommodation_requests" + where,
                "SELECT id,student_user_id,request_type,current_record_id,requested_bed_id,reason,status,"
                        + "reviewed_by,reviewed_at,review_remark,created_at FROM accommodation_requests" + where
                        + " ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?", p, query.getPage(), query.getPageSize(),
                new JdbcDormSupport.Reader<AccommodationRequestDto>() { public AccommodationRequestDto read(ResultSet r) throws SQLException { return JdbcDormSupport.request(r); } });
    }

    @Override
    public AccommodationRequestDto lockRequest(Connection c, long id) throws SQLException {
        String sql = "SELECT id,student_user_id,request_type,current_record_id,requested_bed_id,reason,status,"
                + "reviewed_by,reviewed_at,review_remark,created_at FROM accommodation_requests WHERE id=? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.request(r) : null; }
        }
    }

    @Override
    public AccommodationRequestDto finishRequest(Connection c, long id, long reviewer,
                                                  boolean approved, String remark) throws SQLException {
        String sql = "UPDATE accommodation_requests SET status=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP(3),"
                + "review_remark=? WHERE id=? AND status='PENDING'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, approved ? "APPROVED" : "REJECTED"); s.setLong(2, reviewer);
            s.setString(3, remark); s.setLong(4, id);
            if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.REQUEST_INVALID_STATE, "申请已处理");
        }
        AccommodationRequestDto result = lockRequest(c, id);
        if (result == null) throw new DormRepositoryException(DormCommands.REQUEST_NOT_FOUND, "住宿申请不存在");
        return result;
    }

    private AccommodationDto findOne(Connection c, String predicate, long value, boolean lock) throws SQLException {
        String sql = "SELECT " + RECORD_COLUMNS + RECORD_FROM + " WHERE " + predicate
                + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, value);
            try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.accommodation(r) : null; }
        }
    }

    private static void setLong(PreparedStatement s, int index, Long value) throws SQLException {
        if (value == null) s.setNull(index, java.sql.Types.BIGINT); else s.setLong(index, value.longValue());
    }
}
