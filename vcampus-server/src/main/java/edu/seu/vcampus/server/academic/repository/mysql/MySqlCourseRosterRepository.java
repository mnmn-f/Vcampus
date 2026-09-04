package edu.seu.vcampus.server.academic.repository.mysql;

import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterEntryDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 教师课程花名册的 JDBC 查询。 */
final class MySqlCourseRosterRepository {
    private static final String SQL =
            "SELECT e.id AS enrollment_id,e.student_user_id,e.status AS enrollment_status,"
            + "e.enrolled_at,u.display_name,sp.student_no,sp.college,sp.major,sp.class_name,"
            + "cg.score,cg.remark AS grade_remark "
            + "FROM course_instructors ci "
            + "JOIN enrollments e ON e.course_id=ci.course_id AND e.status<>? "
            + "JOIN users u ON u.id=e.student_user_id "
            + "LEFT JOIN student_profiles sp ON sp.user_id=e.student_user_id "
            + "LEFT JOIN course_grades cg ON cg.enrollment_id=e.id "
            + "WHERE ci.teacher_user_id=? AND ci.course_id=? "
            + "ORDER BY sp.student_no ASC,u.display_name ASC,e.id ASC";

    private MySqlCourseRosterRepository() { }

    static CourseRosterDto find(Connection connection, long teacherId, long courseId)
            throws SQLException {
        List<CourseRosterEntryDto> entries = new ArrayList<CourseRosterEntryDto>();
        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, EnrollmentStatus.DROPPED.name());
            statement.setLong(2, teacherId);
            statement.setLong(3, courseId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) entries.add(read(result));
            }
        }
        return new CourseRosterDto(courseId, entries);
    }

    private static CourseRosterEntryDto read(ResultSet result) throws SQLException {
        return new CourseRosterEntryDto(result.getLong("enrollment_id"),
                result.getLong("student_user_id"), result.getString("student_no"),
                result.getString("display_name"), result.getString("college"),
                result.getString("major"), result.getString("class_name"),
                result.getString("enrollment_status"),
                JdbcTemporal.localDateTime(result.getTimestamp("enrolled_at")),
                result.getBigDecimal("score"), result.getString("grade_remark"));
    }
}
