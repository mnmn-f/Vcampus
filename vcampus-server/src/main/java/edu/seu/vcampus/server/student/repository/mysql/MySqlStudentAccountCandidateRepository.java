package edu.seu.vcampus.server.student.repository.mysql;

import edu.seu.vcampus.common.dto.student.StudentAccountCandidateDto;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidatePage;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidateQuery;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.student.repository.StudentRepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 查询拥有学生角色但尚未建立学籍的账号。 */
final class MySqlStudentAccountCandidateRepository {
    private static final String FROM = " FROM users u WHERE u.status = 'ACTIVE'"
            + " AND EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id"
            + " WHERE ur.user_id = u.id AND r.code = 'STUDENT' AND r.status = 'ACTIVE')"
            + " AND NOT EXISTS (SELECT 1 FROM student_profiles sp WHERE sp.user_id = u.id)";

    StudentAccountCandidatePage search(Connection connection, StudentAccountCandidateQuery query) {
        StudentAccountCandidateQuery safe = query == null
                ? StudentAccountCandidateQuery.firstPage() : query;
        String where = where(safe); List<StudentAccountCandidateDto> items = new ArrayList<>();
        String sql = "SELECT u.username, u.display_name, u.email, u.phone, u.created_at" + FROM
                + where + " ORDER BY u.created_at DESC, u.id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bind(statement, safe, 1); statement.setInt(index++, safe.getPageSize());
            statement.setInt(index, safe.getOffset());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) items.add(new StudentAccountCandidateDto(result.getString(1),
                        result.getString(2), result.getString(3), result.getString(4),
                        JdbcTemporal.localDateTime(result.getTimestamp(5))));
            }
            return new StudentAccountCandidatePage(items, count(connection, safe, where),
                    safe.getPage(), safe.getPageSize());
        } catch (SQLException ex) { throw failure("failed to search pending student accounts", ex); }
    }

    long findId(Connection connection, String account) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT u.id" + FROM + " AND u.username = ?")) {
            statement.setString(1, account);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getLong(1) : 0L; }
        } catch (SQLException ex) { throw failure("failed to resolve pending student account", ex); }
    }

    private long count(Connection c, StudentAccountCandidateQuery q, String where) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("SELECT COUNT(*)" + FROM + where)) {
            bind(statement, q, 1);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getLong(1); }
        }
    }

    private static String where(StudentAccountCandidateQuery q) {
        return q.getKeyword() == null ? "" : " AND (u.username LIKE ? OR u.display_name LIKE ?"
                + " OR u.email LIKE ? OR u.phone LIKE ?)";
    }

    private static int bind(PreparedStatement statement, StudentAccountCandidateQuery q, int index)
            throws SQLException {
        if (q.getKeyword() != null) {
            String value = "%" + q.getKeyword() + "%";
            for (int i = 0; i < 4; i++) statement.setString(index++, value);
        }
        return index;
    }

    private static StudentRepositoryException failure(String message, Throwable cause) {
        return new StudentRepositoryException(message, cause);
    }
}
