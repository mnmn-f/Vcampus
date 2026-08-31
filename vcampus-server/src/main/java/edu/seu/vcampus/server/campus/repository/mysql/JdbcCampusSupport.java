package edu.seu.vcampus.server.campus.repository.mysql;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Campus MySQL 分页、日期绑定和结果集映射工具。 */
final class JdbcCampusSupport {
    private JdbcCampusSupport() { }

    interface Mapper<T> { T map(ResultSet result) throws SQLException; }

    static <T> CampusPage<T> page(Connection c, String countSql, String dataSql,
            List<Object> params, CampusPageQuery query, Mapper<T> mapper) throws SQLException {
        require(c);
        CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        long total;
        try (PreparedStatement s = c.prepareStatement(countSql)) {
            bind(s, params, 1);
            try (ResultSet r = s.executeQuery()) { total = r.next() ? r.getLong(1) : 0L; }
        }
        List<T> rows = new ArrayList<T>();
        try (PreparedStatement s = c.prepareStatement(dataSql)) {
            int index = bind(s, params, 1);
            s.setInt(index++, q.getPageSize());
            s.setLong(index, (long) (q.getPage() - 1) * q.getPageSize());
            try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(mapper.map(r)); }
        }
        return new CampusPage<T>(q.getPage(), q.getPageSize(), total, rows);
    }

    static int bind(PreparedStatement s, List<Object> values, int index) throws SQLException {
        for (Object value : values) s.setObject(index++, value);
        return index;
    }

    static org.threeten.bp.LocalDateTime date(ResultSet r, String column) throws SQLException {
        Timestamp value = r.getTimestamp(column);
        return JdbcTemporal.localDateTime(value);
    }

    static CampusAnnouncementDto announcement(ResultSet r) throws SQLException {
        return new CampusAnnouncementDto(r.getLong("id"), r.getString("module_code"),
                r.getString("title"), r.getString("content"), r.getString("visible_scope"),
                nullableLong(r, "target_role_id"), null, r.getString("status"),
                date(r, "publish_at"), date(r, "expire_at"), r.getLong("publisher_id"));
    }

    static CompetitionDto competition(ResultSet r) throws SQLException {
        int capacity = r.getInt("capacity");
        Integer cap = r.wasNull() ? null : Integer.valueOf(capacity);
        long count = hasColumn(r, "registered_count") ? r.getLong("registered_count") : 0L;
        return new CompetitionDto(r.getLong("id"), r.getString("title"), r.getString("description"),
                r.getLong("organizer_id"), date(r, "start_at"), date(r, "end_at"),
                date(r, "registration_deadline"), cap, r.getString("status"), count);
    }

    static CompetitionRegistrationDto registration(ResultSet r) throws SQLException {
        return new CompetitionRegistrationDto(r.getLong("competition_id"),
                r.getLong("student_user_id"), r.getString("status"),
                date(r, "registered_at"), date(r, "cancelled_at"));
    }

    static SrtpRecordDto srtp(ResultSet r) throws SQLException {
        return new SrtpRecordDto(r.getLong("id"), r.getString("project_code"),
                r.getLong("student_user_id"), r.getString("title"), r.getString("description"),
                r.getBigDecimal("credits"), r.getString("status"), date(r, "submitted_at"),
                nullableLong(r, "reviewed_by"), date(r, "reviewed_at"), r.getString("review_remark"));
    }

    static CampusClassroomDto classroom(ResultSet r) throws SQLException {
        return new CampusClassroomDto(r.getLong("id"), r.getString("building_name"),
                r.getString("room_no"), r.getString("classroom_type"), r.getInt("capacity"),
                r.getString("equipment_description"), r.getString("status"));
    }

    static ClassroomReservationDto reservation(ResultSet r) throws SQLException {
        return new ClassroomReservationDto(r.getLong("id"), r.getLong("classroom_id"),
                r.getString("building_name"), r.getString("room_no"), r.getLong("applicant_id"),
                r.getString("purpose"), date(r, "start_at"), date(r, "end_at"),
                r.getString("status"), nullableLong(r, "reviewed_by"), date(r, "reviewed_at"),
                r.getString("review_remark"));
    }

    static Long nullableLong(ResultSet r, String column) throws SQLException {
        long value = r.getLong(column);
        return r.wasNull() ? null : Long.valueOf(value);
    }

    private static boolean hasColumn(ResultSet r, String name) {
        try { r.findColumn(name); return true; } catch (SQLException ex) { return false; }
    }

    private static void require(Connection c) throws SQLException {
        if (c == null) throw new SQLException("transaction connection is required");
    }
}
