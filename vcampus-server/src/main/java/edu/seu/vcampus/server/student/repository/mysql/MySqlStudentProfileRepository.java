package edu.seu.vcampus.server.student.repository.mysql;

import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.server.student.repository.StudentRepositoryException;
import edu.seu.vcampus.server.student.repository.DelegatingStudentRecordRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** student_profiles/users 的 MySQL DAO 部分。 */
final class MySqlStudentProfileRepository
        implements DelegatingStudentRecordRepository.ProfileStore {
    private static final String PROFILE_COLUMNS =
            "sp.user_id, u.display_name, u.username AS account, sp.student_no, "
                    + "sp.college, sp.major, sp.class_name, sp.enrollment_year, "
                    + "sp.expected_graduation_year, sp.degree_level, sp.gender, "
                    + "sp.birth_date, sp.address, sp.emergency_contact, "
                    + "sp.emergency_phone, sp.status";
    private static final String PROFILE_FROM =
            " FROM student_profiles sp JOIN users u ON u.id = sp.user_id";
    private static final String FIND_PROFILE = "SELECT " + PROFILE_COLUMNS + PROFILE_FROM
            + " WHERE sp.user_id = ?";
    private static final String SEARCH_PROFILE = "SELECT " + PROFILE_COLUMNS + PROFILE_FROM;
    private static final String COUNT_PROFILE = "SELECT COUNT(*)" + PROFILE_FROM;
    private static final String INSERT_PROFILE =
            "INSERT INTO student_profiles (user_id, student_no, college, major, class_name, "
                    + "enrollment_year, expected_graduation_year, degree_level, gender, "
                    + "birth_date, address, emergency_contact, emergency_phone, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_PROFILE =
            "UPDATE student_profiles SET student_no = ?, college = ?, major = ?, class_name = ?, "
                    + "enrollment_year = ?, expected_graduation_year = ?, degree_level = ?, "
                    + "gender = ?, birth_date = ?, address = ?, emergency_contact = ?, "
                    + "emergency_phone = ?, status = ? WHERE user_id = ?";

    @Override
    public StudentProfileDto find(Connection connection, long userId) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_PROFILE)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? MySqlStudentProfileSql.read(result) : null;
            }
        } catch (SQLException ex) {
            throw failure("failed to query student profile", ex);
        }
    }

    @Override
    public StudentProfilePage search(Connection connection, StudentProfileQuery query) {
        String where = MySqlStudentProfileSql.where(query);
        List<StudentProfileDto> items = new ArrayList<StudentProfileDto>();
        try (PreparedStatement statement = connection.prepareStatement(
                SEARCH_PROFILE + where + " ORDER BY sp.student_no LIMIT ? OFFSET ?")) {
            int index = MySqlStudentProfileSql.bindFilters(statement, query, 1);
            statement.setInt(index++, query.getPageSize());
            statement.setInt(index, query.getOffset());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(MySqlStudentProfileSql.read(result));
                }
            }
        } catch (SQLException ex) {
            throw failure("failed to search student profiles", ex);
        }
        long total = count(connection, query, where);
        return new StudentProfilePage(items, total, query.getPage(), query.getPageSize());
    }

    private long count(Connection connection, StudentProfileQuery query, String where) {
        try (PreparedStatement statement = connection.prepareStatement(COUNT_PROFILE + where)) {
            MySqlStudentProfileSql.bindFilters(statement, query, 1);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        } catch (SQLException ex) {
            throw failure("failed to count student profiles", ex);
        }
    }

    @Override
    public boolean userExists(Connection connection, long userId) {
        return exists(connection, "SELECT 1 FROM users WHERE id = ?", userId);
    }

    @Override
    public boolean profileExists(Connection connection, long userId) {
        return exists(connection, "SELECT 1 FROM student_profiles WHERE user_id = ?", userId);
    }

    @Override
    public boolean studentNoExists(Connection connection, String studentNo, long excludedUserId) {
        String sql = "SELECT 1 FROM student_profiles WHERE student_no = ?";
        if (excludedUserId > 0) {
            sql += " AND user_id <> ?";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentNo);
            if (excludedUserId > 0) {
                statement.setLong(2, excludedUserId);
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException ex) {
            throw failure("failed to check student number", ex);
        }
    }

    @Override
    public void insert(Connection connection, StudentProfileWriteRequest request) {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PROFILE)) {
            statement.setLong(1, request.getUserId());
            MySqlStudentProfileSql.bindValues(statement, request, 2);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw failure("failed to insert student profile", ex);
        }
    }

    @Override
    public void update(Connection connection, StudentProfileWriteRequest request) {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PROFILE)) {
            MySqlStudentProfileSql.bindValues(statement, request, 1);
            statement.setLong(14, request.getUserId());
            if (statement.executeUpdate() != 1) {
                throw failure("student profile update affected no row");
            }
        } catch (SQLException ex) {
            throw failure("failed to update student profile", ex);
        }
    }

    private static boolean exists(Connection connection, String sql, long id) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException ex) {
            throw failure("failed to check student data", ex);
        }
    }

    private static StudentRepositoryException failure(String message, Throwable cause) {
        return new StudentRepositoryException(message, cause);
    }

    private static StudentRepositoryException failure(String message) {
        return new StudentRepositoryException(message);
    }
}
