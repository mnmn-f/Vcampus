package edu.seu.vcampus.server.academic.repository.mysql;

import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** 教师、教室可用性和排课时段冲突查询。 */
final class MySqlScheduleRuleRepository {
    boolean allActiveTeachers(Connection c, List<Long> ids) throws SQLException {
        require(c);
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                marks.append(',');
            }
            marks.append('?');
        }
        String sql = "SELECT COUNT(*) FROM teacher_profiles tp JOIN users u ON u.id=tp.user_id "
                + "WHERE tp.status='ACTIVE' AND u.status='ACTIVE' AND tp.user_id IN ("
                + marks + ") AND EXISTS (SELECT 1 FROM user_roles ur "
                + "JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=tp.user_id "
                + "AND r.code='TEACHER' AND r.status='ACTIVE')";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                statement.setLong(i + 1, ids.get(i));
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getLong(1) == ids.size();
            }
        }
    }

    boolean classroomAvailable(Connection c, Long id) throws SQLException {
        require(c);
        if (id == null) {
            return true;
        }
        try (PreparedStatement statement = c.prepareStatement(
                "SELECT status FROM classrooms WHERE id=?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && "AVAILABLE".equals(result.getString(1));
            }
        }
    }

    boolean hasCourseConflict(Connection c, ScheduleSaveRequest r) throws SQLException {
        return hasConflict(c, r, true);
    }

    boolean hasClassroomConflict(Connection c, ScheduleSaveRequest r) throws SQLException {
        return r.getClassroomId() != null && hasConflict(c, r, false);
    }

    private boolean hasConflict(Connection c, ScheduleSaveRequest r, boolean sameCourse)
            throws SQLException {
        require(c);
        StringBuilder sql = new StringBuilder("SELECT 1 FROM course_schedules s WHERE ");
        sql.append(sameCourse ? "s.course_id=?" : "s.classroom_id=?");
        sql.append(" AND s.weekday=? AND s.start_period<=? AND s.end_period>=?");
        sql.append(" AND (? IS NULL OR s.end_date IS NULL OR s.end_date>=?)");
        sql.append(" AND (s.start_date IS NULL OR ? IS NULL OR s.start_date<=?)");
        if (r.isUpdate()) {
            sql.append(" AND s.id<>?");
        }
        try (PreparedStatement statement = c.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, sameCourse ? r.getCourseId() : r.getClassroomId());
            statement.setInt(index++, r.getWeekday());
            statement.setInt(index++, r.getEndPeriod());
            statement.setInt(index++, r.getStartPeriod());
            index = bindDate(statement, index, r.getStartDate());
            index = bindDate(statement, index, r.getEndDate());
            if (r.isUpdate()) {
                statement.setLong(index, r.getScheduleId());
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private int bindDate(PreparedStatement statement, int index,
                         org.threeten.bp.LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index++, java.sql.Types.DATE);
            statement.setNull(index++, java.sql.Types.DATE);
        } else {
            Date date = JdbcTemporal.date(value);
            statement.setDate(index++, date);
            statement.setDate(index++, date);
        }
        return index;
    }

    private static void require(Connection c) throws SQLException {
        if (c == null) {
            throw new SQLException("a transaction connection is required");
        }
    }
}
