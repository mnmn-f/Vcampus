package edu.seu.vcampus.server.student.repository.mysql;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.server.student.repository.StudentRepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 学生本人导出/累计成绩读取，独立于分页和成绩写入 SQL。 */
final class MySqlStudentGradeReadRepository {
    private MySqlStudentGradeReadRepository() {
    }

    static List<StudentGradeDto> findAll(Connection connection, long studentUserId,
                                         String semesterCode, int limit) {
        return findAll(connection, studentUserId, semesterCode, null, limit);
    }

    static List<StudentGradeDto> findAll(Connection connection, long studentUserId,
                                         String semesterCode, Long courseId, int limit) {
        String sql = "SELECT " + MySqlStudentGradeRepository.GRADE_COLUMNS
                + MySqlStudentGradeRepository.GRADE_FROM
                + " WHERE e.student_user_id = ? AND e.status <> 'DROPPED'";
        if (semesterCode != null && semesterCode.trim().length() > 0) {
            sql += " AND c.semester_code = ?";
        }
        if (courseId != null) sql += " AND e.course_id = ?";
        sql += " ORDER BY c.course_code";
        if (limit > 0) sql += " LIMIT ?";
        List<StudentGradeDto> result = new ArrayList<StudentGradeDto>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            int index = 2;
            if (semesterCode != null && semesterCode.trim().length() > 0) {
                statement.setString(index++, semesterCode.trim());
            }
            if (courseId != null) statement.setLong(index++, courseId.longValue());
            if (limit > 0) statement.setInt(index, limit);
            MySqlStudentGradeRepository.readRows(statement, result);
            return result;
        } catch (SQLException ex) {
            throw new StudentRepositoryException("failed to query all student grades", ex);
        }
    }
}
