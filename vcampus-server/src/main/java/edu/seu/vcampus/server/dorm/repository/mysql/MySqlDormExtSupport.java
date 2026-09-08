package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Result-set mappers shared by the split MySQL extension repositories. */
final class MySqlDormExtSupport {
    private MySqlDormExtSupport() { }

    static MeterReadingDto meter(ResultSet r) throws SQLException {
        return new MeterReadingDto(r.getLong("id"), r.getLong("room_id"), r.getString("building_code"),
                r.getString("room_no"), JdbcDormSupport.localDate(r, "period_start"), JdbcDormSupport.localDate(r, "period_end"),
                r.getBigDecimal("electricity_units"), r.getBigDecimal("water_units"), r.getBigDecimal("electricity_price"),
                r.getBigDecimal("water_price"), r.getLong("recorded_by"), JdbcDormSupport.localTimestamp(r, "recorded_at"),
                JdbcDormSupport.longOrNull(r, "bill_id"));
    }

    static HygieneTaskDto task(ResultSet r) throws SQLException {
        return new HygieneTaskDto(r.getLong("id"), r.getLong("room_id"), r.getString("building_code"),
                r.getString("room_no"), r.getString("task_type"), JdbcDormSupport.localDate(r, "plan_date"),
                r.getString("status"), JdbcDormSupport.longOrNull(r, "inspection_id"),
                JdbcDormSupport.longOrNull(r, "source_inspection_id"));
    }

    static VisitorRegistrationDto visitor(ResultSet r) throws SQLException {
        return new VisitorRegistrationDto(r.getLong("id"), r.getLong("student_user_id"), r.getLong("room_id"),
                r.getString("building_code"), r.getString("room_no"), r.getString("visitor_name"),
                VisitorRegistrationDto.mask(r.getString("visitor_id_card")), r.getString("visitor_phone"),
                r.getString("visit_reason"), JdbcDormSupport.localTimestamp(r, "start_at"),
                JdbcDormSupport.localTimestamp(r, "end_at"), JdbcDormSupport.localTimestamp(r, "submitted_at"),
                r.getString("audit_status"), JdbcDormSupport.longOrNull(r, "auditor_id"),
                JdbcDormSupport.localTimestamp(r, "audited_at"), r.getString("audit_remark"));
    }

    static AbsenceWarningDto warning(ResultSet r) throws SQLException {
        return new AbsenceWarningDto(r.getLong("id"), r.getLong("student_user_id"), r.getLong("room_id"),
                r.getString("building_code"), r.getString("room_no"), JdbcDormSupport.localDate(r, "scan_date"),
                JdbcDormSupport.localTimestamp(r, "last_leave_at"), r.getInt("absence_days"), r.getString("warning_level"),
                r.getString("handle_status"), JdbcDormSupport.longOrNull(r, "notified_teacher_id"),
                JdbcDormSupport.localTimestamp(r, "notified_at"), r.getString("note"));
    }

    static RepairEntryPermitDto permit(ResultSet r) throws SQLException {
        boolean allow = r.getBoolean("allow_enter");
        if (r.wasNull()) allow = false;
        return new RepairEntryPermitDto(r.getLong("id"), r.getLong("room_id"), r.getString("building_code"),
                r.getString("room_no"), r.getString("category"), r.getString("status"),
                JdbcDormSupport.localTimestamp(r, "submitted_at"), allow, r.getString("note"),
                r.getString("contact_phone"));
    }

    static NoticeExtraDto notice(ResultSet r) throws SQLException {
        return new NoticeExtraDto(r.getLong("id"), r.getString("title"), r.getString("content"), r.getString("status"),
                JdbcDormSupport.localTimestamp(r, "publish_at"), JdbcDormSupport.localTimestamp(r, "expire_at"),
                r.getLong("publisher_id"), r.getString("notice_type"), r.getString("scope_type"),
                JdbcDormSupport.longOrNull(r, "scope_building_id"), JdbcDormSupport.longOrNull(r, "scope_room_id"),
                r.getString("building_code"), r.getString("room_no"), r.getBoolean("pinned"),
                JdbcDormSupport.localTimestamp(r, "pinned_at"));
    }
}
