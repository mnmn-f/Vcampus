package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import java.sql.*;
import java.util.*;

/** MySQL persistence for student visitor registrations. */
final class MySqlDormVisitorRepository {
    private static final String COLUMNS = "v.id, v.student_user_id, v.room_id, b.building_code, r.room_no, v.visitor_name, v.visitor_id_card, v.visitor_phone, v.visit_reason, v.start_at, v.end_at, v.submitted_at, v.audit_status, v.auditor_id, v.audited_at, v.audit_remark";
    private static final String FROM = " FROM dorm_visitor_registrations v JOIN dorm_rooms r ON r.id = v.room_id JOIN dorm_buildings b ON b.id = r.building_id";

    Long activeRoom(Connection c, long student) throws SQLException {
        String sql = "SELECT bd.room_id FROM accommodation_records ar JOIN dorm_beds bd ON bd.id = ar.bed_id WHERE ar.student_user_id = ? AND ar.status = 'ACTIVE'";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, student); try (ResultSet r = s.executeQuery()) { return r.next() ? Long.valueOf(r.getLong(1)) : null; } }
    }

    VisitorRegistrationDto create(Connection c, long student, long room, VisitorRegistrationRequest request) throws SQLException {
        String sql = "INSERT INTO dorm_visitor_registrations(student_user_id,room_id,visitor_name,visitor_id_card,visitor_phone,visit_reason,start_at,end_at) VALUES(?,?,?,?,?,?,?,?)";
        long id;
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, student); s.setLong(2, room); s.setString(3, request.getVisitorName().trim()); s.setString(4, request.getVisitorIdCard().trim()); s.setString(5, JdbcDormSupport.clean(request.getVisitorPhone())); s.setString(6, request.getVisitReason().trim()); s.setTimestamp(7, JdbcTemporal.timestamp(request.getStartAt())); s.setTimestamp(8, JdbcTemporal.timestamp(request.getEndAt())); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("visitor registration id was not generated"); id = keys.getLong(1); }
        }
        return find(c, id);
    }

    DormPage<VisitorRegistrationDto> list(Connection c, DormPageQuery query, Long student) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query; List<Object> params = new ArrayList<Object>(); StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (student != null) { where.append(" AND v.student_user_id = ?"); params.add(student); } if (q.getRoomId() != null) { where.append(" AND v.room_id = ?"); params.add(q.getRoomId()); }
        String status = JdbcDormSupport.clean(q.getStatus()); if (status != null) { where.append(" AND v.audit_status = ?"); params.add(status); }
        String keyword = JdbcDormSupport.clean(q.getKeyword()); if (keyword != null) { where.append(" AND (v.visitor_name LIKE ? OR v.visit_reason LIKE ? OR r.room_no LIKE ? OR b.building_code LIKE ?)"); String like = "%" + keyword + "%"; for (int i = 0; i < 4; i++) params.add(like); }
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + FROM + where, "SELECT " + COLUMNS + FROM + where + " ORDER BY v.submitted_at DESC, v.id DESC LIMIT ? OFFSET ?", params, q.getPage(), q.getPageSize(), new JdbcDormSupport.Reader<VisitorRegistrationDto>() {
            @Override public VisitorRegistrationDto read(ResultSet r) throws SQLException { return MySqlDormExtSupport.visitor(r); }
        });
    }

    VisitorRegistrationDto find(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE v.id = ?")) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? MySqlDormExtSupport.visitor(r) : null; } }
    }

    VisitorRegistrationDto update(Connection c, long id, String status, Long auditor, org.threeten.bp.LocalDateTime at, String remark) throws SQLException {
        String sql = "UPDATE dorm_visitor_registrations SET audit_status = ?, auditor_id = ?, audited_at = ?, audit_remark = ? WHERE id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setString(1, status); if (auditor == null) s.setNull(2, Types.BIGINT); else s.setLong(2, auditor.longValue()); s.setTimestamp(3, JdbcTemporal.timestamp(at)); s.setString(4, JdbcDormSupport.clean(remark)); s.setLong(5, id); if (s.executeUpdate() == 0) throw new DormRepositoryException(DormExtCommands.VISITOR_NOT_FOUND, "来访登记不存在"); }
        return find(c, id);
    }
}
