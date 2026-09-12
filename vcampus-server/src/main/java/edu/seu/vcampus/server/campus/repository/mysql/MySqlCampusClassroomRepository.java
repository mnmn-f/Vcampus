package edu.seu.vcampus.server.campus.repository.mysql;

import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.server.campus.repository.CampusClassroomRepository;
import edu.seu.vcampus.server.campus.repository.CampusRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** classrooms 与 classroom_reservations 的 MySQL 专责仓储。 */
public final class MySqlCampusClassroomRepository implements CampusClassroomRepository {
    private static final String ROOM_COLUMNS = "id,building_name,room_no,classroom_type,capacity,equipment_description,status";
    private static final String RES_COLUMNS = "r.id,r.classroom_id,c.building_name,c.room_no,r.applicant_id,r.purpose,r.start_at,r.end_at,r.status,r.reviewed_by,r.reviewed_at,r.review_remark,"
            + edu.seu.vcampus.server.db.PersonDisplaySql.label("r.applicant_id") + " applicant_label";
    private static final String RES_FROM = " FROM classroom_reservations r JOIN classrooms c ON c.id=r.classroom_id";

    @Override public CampusPage<CampusClassroomDto> listClassrooms(Connection c, CampusPageQuery q)
            throws SQLException {
        CampusPageQuery query = q == null ? CampusPageQuery.all() : q;
        List<Object> values = new ArrayList<Object>(); String where = " WHERE 1=1";
        if (query.getStatus() != null) { where += " AND status=?"; values.add(query.getStatus()); }
        if (query.getKeyword() != null) { where += " AND (building_name LIKE ? OR room_no LIKE ? OR classroom_type LIKE ?)"; String key = "%" + query.getKeyword() + "%"; values.add(key); values.add(key); values.add(key); }
        return JdbcCampusSupport.page(c, "SELECT COUNT(*) FROM classrooms" + where,
                "SELECT " + ROOM_COLUMNS + " FROM classrooms" + where + " ORDER BY building_name,room_no LIMIT ? OFFSET ?",
                values, query, new JdbcCampusSupport.Mapper<CampusClassroomDto>() { public CampusClassroomDto map(ResultSet r) throws SQLException { return JdbcCampusSupport.classroom(r); } });
    }

    @Override public CampusClassroomDto findClassroom(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + ROOM_COLUMNS + " FROM classrooms WHERE id=?")) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcCampusSupport.classroom(r) : null; }
        }
    }

    @Override public CampusClassroomDto lockClassroom(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + ROOM_COLUMNS + " FROM classrooms WHERE id=? FOR UPDATE")) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcCampusSupport.classroom(r) : null; }
        }
    }

    @Override public CampusPage<ClassroomReservationDto> listReservations(Connection c, Long applicant,
            CampusPageQuery q) throws SQLException {
        CampusPageQuery query = q == null ? CampusPageQuery.all() : q;
        List<Object> values = new ArrayList<Object>(); String where = " WHERE 1=1";
        if (applicant != null) { where += " AND r.applicant_id=?"; values.add(applicant); }
        if (query.getStatus() != null) { where += " AND r.status=?"; values.add(query.getStatus()); }
        if (query.getKeyword() != null) { where += " AND (r.purpose LIKE ? OR c.building_name LIKE ? OR c.room_no LIKE ? OR "
                + edu.seu.vcampus.server.db.PersonDisplaySql.label("r.applicant_id") + " LIKE ?)"; String key = "%" + query.getKeyword() + "%"; values.add(key); values.add(key); values.add(key); values.add(key); }
        return JdbcCampusSupport.page(c, "SELECT COUNT(*)" + RES_FROM + where,
                "SELECT " + RES_COLUMNS + RES_FROM + where + " ORDER BY r.start_at DESC,r.id DESC LIMIT ? OFFSET ?",
                values, query, new JdbcCampusSupport.Mapper<ClassroomReservationDto>() { public ClassroomReservationDto map(ResultSet r) throws SQLException { return JdbcCampusSupport.reservation(r); } });
    }

    @Override public ClassroomReservationDto findReservation(Connection c, long id) throws SQLException { return find(c, id, false); }
    @Override public ClassroomReservationDto lockReservation(Connection c, long id) throws SQLException { return find(c, id, true); }

    @Override public ClassroomReservationDto createReservation(Connection c, ClassroomReservationRequest r,
            long applicant) throws SQLException {
        String sql = "INSERT INTO classroom_reservations(classroom_id,applicant_id,purpose,start_at,end_at,status) VALUES(?,?,?,?,?,'PENDING')";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, r.getClassroomId()); s.setLong(2, applicant); s.setString(3, r.getPurpose()); s.setTimestamp(4, JdbcTemporal.timestamp(r.getStartAt())); s.setTimestamp(5, JdbcTemporal.timestamp(r.getEndAt())); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("reservation id missing"); id = keys.getLong(1); }
        }
        return findReservation(c, id);
    }

    @Override public ClassroomReservationDto review(Connection c, ClassroomReviewRequest r, long reviewer)
            throws SQLException {
        String sql = "UPDATE classroom_reservations SET status=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP(3),review_remark=? WHERE id=? AND status IN ('PENDING','APPROVED')";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, r.getStatus()); s.setLong(2, reviewer); s.setString(3, r.getRemark()); s.setLong(4, r.getReservationId());
            if (s.executeUpdate() != 1) throw new CampusRepositoryException(CampusCommands.CLASSROOM_INVALID_STATE, "预约已处理");
        }
        return findReservation(c, r.getReservationId());
    }

    @Override public ClassroomReservationDto cancelReservation(Connection c, long id, long applicant)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE classroom_reservations SET status='CANCELLED' WHERE id=? AND applicant_id=? AND status IN ('PENDING','APPROVED')")) {
            s.setLong(1, id); s.setLong(2, applicant);
            if (s.executeUpdate() != 1) throw new CampusRepositoryException(CampusCommands.CLASSROOM_INVALID_STATE, "预约不在可取消状态");
        }
        return findReservation(c, id);
    }

    @Override public boolean hasConflict(Connection c, long classroomId, org.threeten.bp.LocalDateTime start,
            org.threeten.bp.LocalDateTime end, long excluded) throws SQLException {
        String sql = "SELECT 1 FROM classroom_reservations WHERE classroom_id=? AND id<>? AND status='APPROVED' AND start_at<? AND end_at>? LIMIT 1 FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, classroomId); s.setLong(2, excluded); s.setTimestamp(3, JdbcTemporal.timestamp(end)); s.setTimestamp(4, JdbcTemporal.timestamp(start)); try (ResultSet r = s.executeQuery()) { return r.next(); } }
    }

    @Override public boolean hasApplicantConflict(Connection c, long applicantId,
            org.threeten.bp.LocalDateTime start, org.threeten.bp.LocalDateTime end) throws SQLException {
        String sql = "SELECT 1 FROM classroom_reservations WHERE applicant_id=? AND status IN ('PENDING','APPROVED') AND start_at<? AND end_at>? LIMIT 1 FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, applicantId); s.setTimestamp(2, JdbcTemporal.timestamp(end)); s.setTimestamp(3, JdbcTemporal.timestamp(start));
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    private ClassroomReservationDto find(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT " + RES_COLUMNS + RES_FROM + " WHERE r.id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcCampusSupport.reservation(r) : null; } }
    }
}
