package edu.seu.vcampus.server.student.repository.mysql;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.academic.GpaScale;
import edu.seu.vcampus.server.student.repository.EnrollmentRecord;
import edu.seu.vcampus.server.student.repository.DelegatingStudentRecordRepository;
import edu.seu.vcampus.server.student.repository.StudentRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
/** enrollments/course_grades/course_instructors 的 MySQL DAO 部分。 */
final class MySqlStudentGradeRepository
        implements DelegatingStudentRecordRepository.GradeStore {
    static final String GRADE_COLUMNS =
            "cg.id AS grade_id, e.id AS enrollment_id, e.student_user_id, "
            + "e.course_id, c.course_code, c.course_name, cg.score, cg.grade_point, "
                    + "c.semester_code, c.credits, cg.gpa_included, cg.recorded_by, "
            + "cg.recorded_at, cg.remark, e.status AS enrollment_status";
    static final String GRADE_FROM =
            " FROM course_grades cg JOIN enrollments e ON e.id = cg.enrollment_id "
                    + "JOIN courses c ON c.id = e.course_id";
    private static final String INSERT_GRADE =
            "INSERT INTO course_grades (enrollment_id, score, grade_point, recorded_by, remark) "
                    + "VALUES (?, ?, ?, ?, ?) AS new ON DUPLICATE KEY UPDATE score = new.score, "
                    + "grade_point = new.grade_point, recorded_by = new.recorded_by, "
            + "remark = new.remark";
    @Override
    public StudentGradePage find(Connection connection, long studentUserId, StudentGradeQuery query) {
        return page(connection, Long.valueOf(studentUserId), query.getCourseId(),
                query.getSemesterCode(),
                query.getPage(), query.getPageSize(), query.getOffset());
    }
    @Override
    public StudentGradePage review(Connection connection, StudentGradeReviewQuery query) {
        return page(connection, query.getStudentUserId(), query.getCourseId(), null,
                query.getPage(), query.getPageSize(), query.getOffset());
    }
    @Override
    public List<StudentGradeDto> findAll(Connection connection, long studentUserId) {
        return MySqlStudentGradeReadRepository.findAll(connection, studentUserId, null, 0);
    }
    @Override
    public List<StudentGradeDto> findAll(Connection connection, long studentUserId,
                                         String semesterCode, int limit) {
        return MySqlStudentGradeReadRepository.findAll(connection, studentUserId,
                semesterCode, limit);
    }

    @Override
    public List<StudentGradeDto> findAll(Connection connection, long studentUserId,
                                         String semesterCode, Long courseId, int limit) {
        return MySqlStudentGradeReadRepository.findAll(connection, studentUserId,
                semesterCode, courseId, limit);
    }
    @Override
    public StudentGradeDto findByEnrollment(Connection connection, long enrollmentId) {
        String sql = GRADE_FROM + " WHERE e.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, enrollmentId);
            List<StudentGradeDto> rows = new ArrayList<StudentGradeDto>();
            readRows(statement, rows);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (SQLException ex) {
            throw failure("failed to query grade", ex);
        }
    }
    @Override
    public EnrollmentRecord findEnrollment(Connection connection, long enrollmentId) {
        String sql = "SELECT id, student_user_id, course_id, status FROM enrollments "
                + "WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, enrollmentId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new EnrollmentRecord(
                        result.getLong("id"), result.getLong("student_user_id"),
                        result.getLong("course_id"), result.getString("status"))
                        : null;
            }
        } catch (SQLException ex) {
            throw failure("failed to query enrollment", ex);
        }
    }
    @Override
    public boolean teacherOwnsCourse(Connection connection, long teacherUserId, long courseId) {
        String sql = "SELECT 1 FROM course_instructors WHERE course_id = ? AND teacher_user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, teacherUserId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException ex) {
            throw failure("failed to check course instructor", ex);
        }
    }
    @Override
    public void upsert(Connection connection, long recorderUserId,
                StudentGradeRecordRequest request) {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_GRADE)) {
            statement.setLong(1, request.getEnrollmentId());
            statement.setBigDecimal(2, request.getScore());
            statement.setBigDecimal(3, GpaScale.point(request.getScore()));
            statement.setLong(4, recorderUserId);
            if (request.getRemark() == null || request.getRemark().trim().isEmpty()) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, request.getRemark().trim());
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw failure("failed to save course grade", ex);
        }
    }
    private StudentGradePage page(Connection connection, Long studentUserId, Long courseId,
                                  String semesterCode,
                                  int page, int pageSize, int offset) {
        String where = gradeWhere(studentUserId, courseId, semesterCode);
        String sql = GRADE_FROM + where + " AND e.status <> 'DROPPED'"
                + " ORDER BY c.course_code, e.id LIMIT ? OFFSET ?";
        List<StudentGradeDto> items = new ArrayList<StudentGradeDto>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindFilters(statement, studentUserId, courseId, semesterCode, 1);
            statement.setInt(index++, pageSize);
            statement.setInt(index, offset);
            readRows(statement, items);
        } catch (SQLException ex) {
            throw failure("failed to query grades", ex);
        }
        return new StudentGradePage(items, count(connection, studentUserId, courseId,
                semesterCode),
                page, pageSize);
    }
    private long count(Connection connection, Long studentUserId, Long courseId,
                       String semesterCode) {
        String sql = "SELECT COUNT(*)" + GRADE_FROM + gradeWhere(studentUserId, courseId,
                semesterCode)
                + " AND e.status <> 'DROPPED'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFilters(statement, studentUserId, courseId, semesterCode, 1);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        } catch (SQLException ex) {
            throw failure("failed to count grades", ex);
        }
    }
    private static String gradeWhere(Long studentUserId, Long courseId,
                                     String semesterCode) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (studentUserId != null) where.append(" AND e.student_user_id = ?");
        if (courseId != null) where.append(" AND e.course_id = ?");
        if (semesterCode != null && semesterCode.trim().length() > 0) {
            where.append(" AND c.semester_code = ?");
        }
        return where.toString();
    }
    private static int bindFilters(PreparedStatement statement, Long studentUserId,
                                   Long courseId, String semesterCode, int index)
            throws SQLException {
        if (studentUserId != null) statement.setLong(index++, studentUserId);
        if (courseId != null) statement.setLong(index++, courseId);
        if (semesterCode != null && semesterCode.trim().length() > 0) {
            statement.setString(index++, semesterCode.trim());
        }
        return index;
    }
    static void readRows(PreparedStatement statement, List<StudentGradeDto> output)
            throws SQLException {
        try (ResultSet result = statement.executeQuery()) {
            while (result.next()) output.add(readGrade(result));
        }
    }
    private static StudentGradeDto readGrade(ResultSet result) throws SQLException {
        Timestamp recordedAt = result.getTimestamp("recorded_at");
        BigDecimal score = result.getBigDecimal("score");
        return new StudentGradeDto(result.getLong("grade_id"),
                result.getLong("enrollment_id"), result.getLong("student_user_id"),
                result.getLong("course_id"), result.getString("course_code"),
                result.getString("course_name"), score, GpaScale.point(score),
                result.getLong("recorded_by"),
                JdbcTemporal.localDateTime(recordedAt),
                result.getString("remark"), result.getString("enrollment_status"),
                result.getString("semester_code"), result.getBigDecimal("credits"),
                result.getBoolean("gpa_included"));
    }
    private static StudentRepositoryException failure(String message, Throwable cause) {
        return new StudentRepositoryException(message, cause);
    }
}
