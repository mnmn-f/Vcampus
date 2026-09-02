package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.dorm.repository.ResidentAbsenceSnapshot;
import java.sql.*;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** MySQL persistence for absence warnings and threshold configuration. */
final class MySqlDormWarningRepository {
    private static final String COLUMNS = "w.id, w.student_user_id, w.room_id, b.building_code, r.room_no, w.scan_date, w.last_leave_at, w.absence_days, w.warning_level, w.handle_status, w.notified_teacher_id, w.notified_at, w.note";
    private static final String FROM = " FROM dorm_absence_warnings w JOIN dorm_rooms r ON r.id = w.room_id JOIN dorm_buildings b ON b.id = r.building_id";

    List<ResidentAbsenceSnapshot> residents(Connection c) throws SQLException {
        String sql = "SELECT ar.student_user_id, bd.room_id, (SELECT MAX(x.occurred_at) FROM access_records x WHERE x.student_user_id = ar.student_user_id AND x.record_type = 'EXIT') AS last_exit_at, (SELECT MAX(x.occurred_at) FROM access_records x WHERE x.student_user_id = ar.student_user_id AND x.record_type = 'ENTRY') AS last_entry_at FROM accommodation_records ar JOIN dorm_beds bd ON bd.id = ar.bed_id WHERE ar.status = 'ACTIVE' ORDER BY ar.student_user_id";
        List<ResidentAbsenceSnapshot> rows = new ArrayList<ResidentAbsenceSnapshot>();
        try (PreparedStatement s = c.prepareStatement(sql); ResultSet r = s.executeQuery()) { while (r.next()) rows.add(new ResidentAbsenceSnapshot(r.getLong("student_user_id"), r.getLong("room_id"), JdbcDormSupport.localTimestamp(r, "last_exit_at"), JdbcDormSupport.localTimestamp(r, "last_entry_at"))); }
        return rows;
    }

    boolean hasApprovedLeave(Connection c, long student, LocalDate date) throws SQLException {
        String sql = "SELECT 1 FROM leave_requests WHERE student_user_id = ? AND status = 'APPROVED' AND start_at <= ? AND end_at >= ? LIMIT 1";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, student); s.setTimestamp(2, JdbcTemporal.timestamp(date.atTime(23, 59, 59))); s.setTimestamp(3, JdbcTemporal.timestamp(date.atStartOfDay())); try (ResultSet r = s.executeQuery()) { return r.next(); } }
    }

    AbsenceWarningDto save(Connection c, long student, long room, LocalDate date, LocalDateTime last, int days, String level) throws SQLException {
        String sql = "INSERT INTO dorm_absence_warnings(student_user_id,room_id,scan_date,last_leave_at,absence_days,warning_level) VALUES(?,?,?,?,?,?) AS incoming ON DUPLICATE KEY UPDATE room_id = incoming.room_id, last_leave_at = incoming.last_leave_at, absence_days = incoming.absence_days, warning_level = incoming.warning_level";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, student); s.setLong(2, room); s.setDate(3, JdbcTemporal.date(date)); s.setTimestamp(4, JdbcTemporal.timestamp(last)); s.setInt(5, days); s.setString(6, level); s.executeUpdate(); }
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE w.student_user_id = ? AND w.scan_date = ?")) { s.setLong(1, student); s.setDate(2, JdbcTemporal.date(date)); try (ResultSet r = s.executeQuery()) { if (!r.next()) throw new DormRepositoryException(DormExtCommands.WARNING_NOT_FOUND, "预警写入后无法读回"); return MySqlDormExtSupport.warning(r); } }
    }

    DormPage<AbsenceWarningDto> list(Connection c, DormPageQuery query) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query; List<Object> params = new ArrayList<Object>(); StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q.getRoomId() != null) { where.append(" AND w.room_id = ?"); params.add(q.getRoomId()); } if (q.getBuildingId() != null) { where.append(" AND r.building_id = ?"); params.add(q.getBuildingId()); }
        String status = JdbcDormSupport.clean(q.getStatus()); if (status != null) { where.append(" AND w.handle_status = ?"); params.add(status); }
        String keyword = JdbcDormSupport.clean(q.getKeyword()); if (keyword != null) { where.append(" AND (r.room_no LIKE ? OR b.building_code LIKE ? OR w.warning_level LIKE ? OR CAST(w.student_user_id AS CHAR) LIKE ?)"); String like = "%" + keyword + "%"; for (int i = 0; i < 4; i++) params.add(like); }
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + FROM + where, "SELECT " + COLUMNS + FROM + where + " ORDER BY w.scan_date DESC, w.absence_days DESC, w.id DESC LIMIT ? OFFSET ?", params, q.getPage(), q.getPageSize(), new JdbcDormSupport.Reader<AbsenceWarningDto>() {
            @Override public AbsenceWarningDto read(ResultSet r) throws SQLException { return MySqlDormExtSupport.warning(r); }
        });
    }

    AbsenceWarningDto find(Connection c, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE w.id = ?")) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? MySqlDormExtSupport.warning(r) : null; } }
    }

    AbsenceWarningDto update(Connection c, long id, String status, Long teacher, LocalDateTime at, String note) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE dorm_absence_warnings SET handle_status = ?, notified_teacher_id = ?, notified_at = ?, note = ? WHERE id = ?")) { s.setString(1, status); if (teacher == null) s.setNull(2, Types.BIGINT); else s.setLong(2, teacher.longValue()); s.setTimestamp(3, JdbcTemporal.timestamp(at)); s.setString(4, JdbcDormSupport.clean(note)); s.setLong(5, id); if (s.executeUpdate() == 0) throw new DormRepositoryException(DormExtCommands.WARNING_NOT_FOUND, "预警不存在"); }
        return find(c, id);
    }

    WarningConfigDto config(Connection c) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT warn_days, notify_days, exempt_on_leave, updated_at FROM dorm_warning_configs WHERE id = 1"); ResultSet r = s.executeQuery()) {
            if (!r.next()) throw new DormRepositoryException(DormExtCommands.CONFIG_INVALID, "未归预警阈值未初始化，请先执行 V3 迁移");
            return new WarningConfigDto(r.getInt("warn_days"), r.getInt("notify_days"), r.getBoolean("exempt_on_leave"), JdbcDormSupport.localTimestamp(r, "updated_at"));
        }
    }

    WarningConfigDto saveConfig(Connection c, WarningConfigRequest request, long actor) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE dorm_warning_configs SET warn_days = ?, notify_days = ?, exempt_on_leave = ?, updated_by = ? WHERE id = 1")) { s.setInt(1, request.getWarnDays()); s.setInt(2, request.getNotifyDays()); s.setBoolean(3, request.isExemptOnLeave()); s.setLong(4, actor); s.executeUpdate(); }
        return config(c);
    }

    List<AbsenceWarningDto> pendingSevere(Connection c, LocalDate date) throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM + " WHERE w.warning_level = ? AND w.handle_status = ? AND w.scan_date <= ? ORDER BY w.absence_days DESC, w.id ASC";
        List<AbsenceWarningDto> rows = new ArrayList<AbsenceWarningDto>();
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setString(1, AbsenceWarningDto.LEVEL_SEVERE); s.setString(2, AbsenceWarningDto.STATUS_PENDING); s.setDate(3, JdbcTemporal.date(date == null ? LocalDate.now() : date)); try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(MySqlDormExtSupport.warning(r)); } }
        return rows;
    }
}
