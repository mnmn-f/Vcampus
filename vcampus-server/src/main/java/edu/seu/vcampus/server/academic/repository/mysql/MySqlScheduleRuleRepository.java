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

    void lockSchedules(Connection c) throws SQLException {
        require(c);
        try (PreparedStatement statement = c.prepareStatement(
                "SELECT id FROM course_schedules FOR UPDATE")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) { /* lock the complete schedule snapshot */ }
            }
        }
    }

    boolean classroomFitsCourse(Connection c, long courseId, Long id) throws SQLException {
        require(c);
        if (id == null) return true;
        String sql = "SELECT c.capacity,c.description,r.classroom_type,r.capacity,r.status "
                + "FROM courses c JOIN classrooms r ON r.id=? WHERE c.id=?";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, courseId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !"AVAILABLE".equals(result.getString(5))
                        || result.getInt(4) < result.getInt(1)) return false;
                String description = result.getString(2);
                String required = requiredRoomType(description);
                return required == null || required.equalsIgnoreCase(result.getString(3));
            }
        }
    }

    private boolean hasConflict(Connection c, ScheduleSaveRequest r, boolean sameCourse)
            throws SQLException {
        require(c);
        StringBuilder sql = new StringBuilder("SELECT 1 FROM course_schedules s WHERE ");
        if (sameCourse) {
            sql.append("(s.course_id=? OR EXISTS (SELECT 1 FROM course_instructors old_i "
                    + "JOIN course_instructors new_i ON new_i.teacher_user_id=old_i.teacher_user_id "
                    + "WHERE old_i.course_id=s.course_id AND new_i.course_id=?) "
                    + "OR EXISTS (SELECT 1 FROM enrollments old_e "
                    + "JOIN enrollments new_e ON new_e.student_user_id=old_e.student_user_id "
                    + "WHERE old_e.course_id=s.course_id AND new_e.course_id=? "
                    + "AND old_e.status='ENROLLED' AND new_e.status='ENROLLED'))");
        } else {
            sql.append("s.classroom_id=?");
        }
        sql.append(" AND s.weekday=? AND s.start_period<=? AND s.end_period>=?");
        sql.append(" AND (? IS NULL OR s.end_date IS NULL OR s.end_date>=?)");
        sql.append(" AND (s.start_date IS NULL OR ? IS NULL OR s.start_date<=?)");
        if (r.isUpdate()) {
            sql.append(" AND s.id<>?");
        }
        try (PreparedStatement statement = c.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, sameCourse ? r.getCourseId() : r.getClassroomId());
            if (sameCourse) {
                statement.setLong(index++, r.getCourseId());
                statement.setLong(index++, r.getCourseId());
            }
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

    private static String requiredRoomType(String description) {
        if (description == null) return null;
        if (description.contains("【机房】") || description.contains("实验室")) return "LAB";
        return description.contains("【会议室】") ? "MEETING" : null;
    }
}
