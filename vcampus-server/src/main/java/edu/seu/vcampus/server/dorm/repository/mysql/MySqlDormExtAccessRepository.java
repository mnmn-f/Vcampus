package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.dorm.service.DormStayRules;
import java.sql.*;
import java.util.*;
import org.threeten.bp.LocalTime;
import org.threeten.bp.LocalDateTime;

/** MySQL persistence for stay state, access policy and repair-entry permits. */
final class MySqlDormExtAccessRepository {
    private static final String PERMIT_SELECT = "SELECT o.id, o.room_id, b.building_code, r.room_no, o.category, o.status, o.submitted_at, p.allow_enter, p.note, u.phone AS contact_phone";
    private static final String PERMIT_FROM = " FROM repair_orders o JOIN dorm_rooms r ON r.id = o.room_id JOIN dorm_buildings b ON b.id = r.building_id LEFT JOIN dorm_repair_entry_permits p ON p.repair_order_id = o.id JOIN users u ON u.id = o.reporter_id";

    List<StayStatusDto> stayRows(Connection c, Long student) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ar.student_user_id, bd.room_id, b.building_code, r.room_no,"
                + " (SELECT MAX(x.occurred_at) FROM access_records x WHERE x.student_user_id = ar.student_user_id AND x.record_type = 'EXIT') AS last_exit_at,"
                + " (SELECT MAX(x.occurred_at) FROM access_records x WHERE x.student_user_id = ar.student_user_id AND x.record_type = 'ENTRY') AS last_entry_at"
                + " FROM accommodation_records ar JOIN dorm_beds bd ON bd.id = ar.bed_id JOIN dorm_rooms r ON r.id = bd.room_id JOIN dorm_buildings b ON b.id = r.building_id WHERE ar.status = 'ACTIVE'");
        if (student != null) sql.append(" AND ar.student_user_id = ?"); sql.append(" ORDER BY ar.student_user_id");
        List<StayStatusDto> rows = new ArrayList<StayStatusDto>();
        try (PreparedStatement s = c.prepareStatement(sql.toString())) { if (student != null) s.setLong(1, student.longValue()); try (ResultSet r = s.executeQuery()) {
            while (r.next()) rows.add(new StayStatusDto(r.getLong("student_user_id"), r.getLong("room_id"), r.getString("building_code"), r.getString("room_no"), null,
                    JdbcDormSupport.localTimestamp(r, "last_exit_at"), JdbcDormSupport.localTimestamp(r, "last_entry_at")));
        } }
        return rows;
    }

    AccessPolicyDto policy(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT curfew_time, dawn_time, updated_at FROM dorm_access_policies WHERE id = 1"); ResultSet r = s.executeQuery()) {
            if (!r.next()) throw new DormRepositoryException(DormExtCommands.POLICY_INVALID, "门禁策略未初始化，请先执行 V3 迁移");
            return new AccessPolicyDto(JdbcTemporal.localTime(r.getTime("curfew_time")), JdbcTemporal.localTime(r.getTime("dawn_time")), JdbcDormSupport.localTimestamp(r, "updated_at"));
        }
    }

    AccessPolicyDto savePolicy(Connection c, AccessPolicyRequest request, long actor) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE dorm_access_policies SET curfew_time = ?, dawn_time = ?, updated_by = ? WHERE id = 1")) {
            s.setTime(1, JdbcTemporal.time(request.getCurfewTime())); s.setTime(2, JdbcTemporal.time(request.getDawnTime())); s.setLong(3, actor); s.executeUpdate();
        }
        return policy(c);
    }

    DormPage<AccessRecordExtDto> access(Connection c, long student, DormPageQuery query, final AccessPolicyDto policy) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>(); params.add(Long.valueOf(student));
        StringBuilder where = new StringBuilder(" WHERE a.student_user_id = ?");
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) { where.append(" AND (a.door_name LIKE ? OR a.record_type LIKE ?)"); String like = "%" + keyword + "%"; params.add(like); params.add(like); }
        String type = JdbcDormSupport.clean(q.getStatus()); if (type != null) { where.append(" AND a.record_type = ?"); params.add(type); }
        String from = " FROM access_records a";
        String count = "SELECT COUNT(*)" + from + where;
        String data = "SELECT a.id, a.student_user_id, a.record_type, a.occurred_at, a.door_name" + from + where + " ORDER BY a.occurred_at DESC, a.id DESC LIMIT ? OFFSET ?";
        final LocalTime curfew = policy.getCurfewTime(); final LocalTime dawn = policy.getDawnTime();
        return JdbcDormSupport.page(c, count, data, params, q.getPage(), q.getPageSize(), new JdbcDormSupport.Reader<AccessRecordExtDto>() {
            @Override public AccessRecordExtDto read(ResultSet r) throws SQLException {
                String type = r.getString("record_type"); LocalDateTime at = JdbcDormSupport.localTimestamp(r, "occurred_at");
                return new AccessRecordExtDto(r.getLong("id"), r.getLong("student_user_id"), type, at, r.getString("door_name"), DormStayRules.isLateReturn(type, at, curfew, dawn));
            }
        });
    }

    Long repairReporter(Connection c, long order) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT reporter_id FROM repair_orders WHERE id = ?")) { s.setLong(1, order); try (ResultSet r = s.executeQuery()) { return r.next() ? Long.valueOf(r.getLong(1)) : null; } }
    }

    String reporterPhone(Connection c, long order) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT u.phone FROM repair_orders o JOIN users u ON u.id = o.reporter_id WHERE o.id = ?")) { s.setLong(1, order); try (ResultSet r = s.executeQuery()) { return r.next() ? r.getString(1) : null; } }
    }

    RepairEntryPermitDto savePermit(Connection c, RepairEntryPermitRequest request, long actor) throws SQLException {
        String sql = "INSERT INTO dorm_repair_entry_permits(repair_order_id,allow_enter,note,updated_by) VALUES(?,?,?,?) AS incoming ON DUPLICATE KEY UPDATE allow_enter = incoming.allow_enter, note = incoming.note, updated_by = incoming.updated_by";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, request.getRepairOrderId()); s.setBoolean(2, request.isAllowEnter()); s.setString(3, JdbcDormSupport.clean(request.getNote())); s.setLong(4, actor); s.executeUpdate(); }
        try (PreparedStatement s = c.prepareStatement(PERMIT_SELECT + PERMIT_FROM + " WHERE o.id = ?")) { s.setLong(1, request.getRepairOrderId()); try (ResultSet r = s.executeQuery()) {
            if (!r.next()) throw new DormRepositoryException(DormExtCommands.REPAIR_NOT_FOUND, "报修单不存在"); return MySqlDormExtSupport.permit(r);
        } }
    }

    DormPage<RepairEntryPermitDto> permits(Connection c, long student, DormPageQuery query) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query; List<Object> params = new ArrayList<Object>(); params.add(Long.valueOf(student));
        StringBuilder where = new StringBuilder(" WHERE o.reporter_id = ?"); String status = JdbcDormSupport.clean(q.getStatus());
        if (status != null) { where.append(" AND o.status = ?"); params.add(status); }
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + PERMIT_FROM + where, PERMIT_SELECT + PERMIT_FROM + where + " ORDER BY o.submitted_at DESC, o.id DESC LIMIT ? OFFSET ?", params, q.getPage(), q.getPageSize(), new JdbcDormSupport.Reader<RepairEntryPermitDto>() {
            @Override public RepairEntryPermitDto read(ResultSet r) throws SQLException { return MySqlDormExtSupport.permit(r); }
        });
    }

    int activeResidentCount(Connection c, long room) throws SQLException {
        return count(c, "SELECT COUNT(*) FROM accommodation_records ar JOIN dorm_beds bd ON bd.id = ar.bed_id WHERE bd.room_id = ? AND ar.status = 'ACTIVE'", room, 1);
    }

    int roomReferenceCount(Connection c, long room) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM accommodation_records ar JOIN dorm_beds bd ON bd.id = ar.bed_id WHERE bd.room_id = ?)"
                + "+(SELECT COUNT(*) FROM utility_bills WHERE room_id = ?)+(SELECT COUNT(*) FROM hygiene_inspections WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM repair_orders WHERE room_id = ?)+(SELECT COUNT(*) FROM dorm_meter_readings WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM dorm_hygiene_tasks WHERE room_id = ?)+(SELECT COUNT(*) FROM dorm_absence_warnings WHERE room_id = ?)"
                + "+(SELECT COUNT(*) FROM dorm_visitor_registrations WHERE room_id = ?)";
        return count(c, sql, room, 8);
    }

    void deleteRoom(Connection c, long room) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("DELETE FROM dorm_beds WHERE room_id = ?")) { s.setLong(1, room); s.executeUpdate(); }
        try (PreparedStatement s = c.prepareStatement("DELETE FROM dorm_rooms WHERE id = ?")) { s.setLong(1, room); if (s.executeUpdate() == 0) throw new DormRepositoryException(DormExtCommands.ROOM_NOT_FOUND, "房间不存在"); }
    }

    private static int count(Connection c, String sql, long room, int placeholders) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) { for (int i = 1; i <= placeholders; i++) s.setLong(i, room); try (ResultSet r = s.executeQuery()) { return r.next() ? r.getInt(1) : 0; } }
    }
}
