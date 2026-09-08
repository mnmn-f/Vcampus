package edu.seu.vcampus.server.academic.repository.mysql;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentDto;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentListDto;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** enrollments 及学生/教师业务查询的 JDBC 操作。 */
final class MySqlEnrollmentRepository {
    private final MySqlCourseRepository courses;

    MySqlEnrollmentRepository(MySqlCourseRepository courses) {
        this.courses = courses;
    }

    boolean isActiveStudent(Connection c, long userId) throws SQLException {
        requireConnection(c);
        String sql = "SELECT 1 FROM student_profiles sp JOIN users u ON u.id=sp.user_id "
                + "WHERE sp.user_id=? AND sp.status='ENROLLED' AND u.status='ACTIVE'";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    EnrollmentDto findEnrollment(Connection c, long studentId, long courseId,
                                 boolean forUpdate) throws SQLException {
        requireConnection(c);
        String sql = "SELECT id,student_user_id,course_id,status,enrolled_at,dropped_at "
                + "FROM enrollments WHERE student_user_id=? AND course_id=?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            statement.setLong(2, courseId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readEnrollment(result) : null;
            }
        }
    }

    long countEnrolled(Connection c, long courseId) throws SQLException {
        requireConnection(c);
        try (PreparedStatement statement = c.prepareStatement(
                "SELECT COUNT(*) FROM enrollments WHERE course_id=? AND status='ENROLLED'")) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }
    }

    boolean teacherOwnsCourse(Connection c, long teacherId, long courseId)
            throws SQLException {
        requireConnection(c);
        String sql = "SELECT 1 FROM course_instructors WHERE teacher_user_id=? "
                + "AND course_id=?";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, teacherId);
            statement.setLong(2, courseId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    CourseRosterDto findCourseRoster(Connection c, long teacherId, long courseId)
            throws SQLException {
        requireConnection(c);
        return MySqlCourseRosterRepository.find(c, teacherId, courseId);
    }

    boolean hasStudentScheduleConflict(Connection c, long studentId, long courseId)
            throws SQLException {
        requireConnection(c);
        String sql = "SELECT 1 FROM enrollments e "
                + "JOIN course_schedules old_s ON old_s.course_id=e.course_id "
                + "JOIN course_schedules new_s ON new_s.course_id=? "
                + "WHERE e.student_user_id=? AND e.status='ENROLLED' AND e.course_id<>? "
                + "AND old_s.weekday=new_s.weekday "
                + "AND old_s.start_period<=new_s.end_period "
                + "AND old_s.end_period>=new_s.start_period "
                + "AND (old_s.start_date IS NULL OR new_s.end_date IS NULL "
                + "OR old_s.start_date<=new_s.end_date) "
                + "AND (old_s.end_date IS NULL OR new_s.start_date IS NULL "
                + "OR old_s.end_date>=new_s.start_date) LIMIT 1";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, studentId);
            statement.setLong(3, courseId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    EnrollmentDto enroll(Connection c, long studentId, long courseId) throws SQLException {
        requireConnection(c);
        EnrollmentDto old = findEnrollment(c, studentId, courseId, true);
        if (old == null) {
            String sql = "INSERT INTO enrollments(student_user_id,course_id,status) "
                    + "VALUES(?,?,'ENROLLED')";
            try (PreparedStatement statement = c.prepareStatement(sql)) {
                statement.setLong(1, studentId);
                statement.setLong(2, courseId);
                statement.executeUpdate();
            }
        } else {
            String sql = "UPDATE enrollments SET status='ENROLLED',enrolled_at=CURRENT_TIMESTAMP(3),"
                    + "dropped_at=NULL,version=version+1 WHERE id=?";
            try (PreparedStatement statement = c.prepareStatement(sql)) {
                statement.setLong(1, old.getId());
                statement.executeUpdate();
            }
        }
        EnrollmentDto result = findEnrollment(c, studentId, courseId, false);
        if (result == null) {
            throw new SQLException("enrollment disappeared after insert");
        }
        return result;
    }

    void drop(Connection c, long studentId, long courseId) throws SQLException {
        requireConnection(c);
        String sql = "UPDATE enrollments SET status='DROPPED',dropped_at=CURRENT_TIMESTAMP(3),"
                + "version=version+1 WHERE student_user_id=? AND course_id=?"
                + " AND status='ENROLLED'";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            statement.setLong(2, courseId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("enrollment is not active");
            }
        }
    }

    StudentScheduleDto findStudentSchedule(Connection c, long studentId) throws SQLException {
        return findStudentSchedule(c, studentId, StudentScheduleQuery.all());
    }

    StudentScheduleDto findStudentSchedule(Connection c, long studentId,
                                            StudentScheduleQuery query) throws SQLException {
        requireConnection(c);
        String semesterCode = query == null ? null : query.getSemesterCode();
        String sql = "SELECT e.course_id FROM enrollments e JOIN courses c "
                + "ON c.id=e.course_id WHERE e.student_user_id=? AND e.status='ENROLLED'";
        if (semesterCode != null) sql += " AND c.semester_code=?";
        sql += " ORDER BY e.course_id";
        List<Long> ids = new ArrayList<Long>();
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            if (semesterCode != null) statement.setString(2, semesterCode);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ids.add(result.getLong(1));
                }
            }
        }
        List<CourseDto> coursesList = new ArrayList<CourseDto>();
        for (Long id : ids) {
            CourseDto course = courses.findCourse(c, id.longValue(), false);
            if (course != null) {
                coursesList.add(course);
            }
        }
        return new StudentScheduleDto(studentId, semesterCode, coursesList);
    }

    StudentEnrollmentListDto findStudentEnrollments(Connection c, long studentId)
            throws SQLException {
        requireConnection(c);
        String sql = "SELECT id,student_user_id,course_id,status,enrolled_at,dropped_at "
                + "FROM enrollments WHERE student_user_id=? ORDER BY enrolled_at DESC,id DESC";
        List<StudentEnrollmentDto> items = new ArrayList<StudentEnrollmentDto>();
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    EnrollmentDto enrollment = readEnrollment(result);
                    CourseDto course = courses.findCourse(c, enrollment.getCourseId(), false);
                    if (course != null) items.add(new StudentEnrollmentDto(enrollment, course));
                }
            }
        }
        return new StudentEnrollmentListDto(items);
    }

    private EnrollmentDto readEnrollment(ResultSet result) throws SQLException {
        Timestamp enrolled = result.getTimestamp("enrolled_at");
        Timestamp dropped = result.getTimestamp("dropped_at");
        return new EnrollmentDto(result.getLong("id"), result.getLong("student_user_id"),
                result.getLong("course_id"), result.getString("status"),
                JdbcTemporal.localDateTime(enrolled),
                JdbcTemporal.localDateTime(dropped));
    }

    private static void requireConnection(Connection c) throws SQLException {
        if (c == null) {
            throw new SQLException("a transaction connection is required");
        }
    }
}
