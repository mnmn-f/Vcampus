package edu.seu.vcampus.server.student.repository.mysql;

import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** student_profiles 查询条件、字段绑定和结果映射。 */
final class MySqlStudentProfileSql {
    private MySqlStudentProfileSql() {
    }

    static String where(StudentProfileQuery query) {
        StringBuilder result = new StringBuilder(" WHERE 1 = 1");
        if (query.getStudentNo() != null) result.append(" AND sp.student_no LIKE ?");
        if (query.getDisplayName() != null) result.append(" AND u.display_name LIKE ?");
        if (query.getCollege() != null) result.append(" AND sp.college LIKE ?");
        if (query.getMajor() != null) result.append(" AND sp.major LIKE ?");
        if (query.getClassName() != null) result.append(" AND sp.class_name LIKE ?");
        if (query.getStatus() != null) result.append(" AND sp.status = ?");
        return result.toString();
    }

    static int bindFilters(PreparedStatement statement, StudentProfileQuery query, int index)
            throws SQLException {
        index = bindLike(statement, query.getStudentNo(), index);
        index = bindLike(statement, query.getDisplayName(), index);
        index = bindLike(statement, query.getCollege(), index);
        index = bindLike(statement, query.getMajor(), index);
        index = bindLike(statement, query.getClassName(), index);
        if (query.getStatus() != null) statement.setString(index++, query.getStatus().name());
        return index;
    }

    static void bindValues(PreparedStatement statement, StudentProfileWriteRequest request,
                           int index) throws SQLException {
        statement.setString(index++, request.getStudentNo());
        setString(statement, index++, request.getCollege());
        setString(statement, index++, request.getMajor());
        setString(statement, index++, request.getClassName());
        setInteger(statement, index++, request.getEnrollmentYear());
        setInteger(statement, index++, request.getExpectedGraduationYear());
        setString(statement, index++, request.getDegreeLevel());
        setString(statement, index++, request.getGender());
        if (request.getBirthDate() == null) statement.setNull(index++, Types.DATE);
        else statement.setDate(index++, JdbcTemporal.date(request.getBirthDate()));
        setString(statement, index++, request.getAddress());
        setString(statement, index++, request.getEmergencyContact());
        setString(statement, index++, request.getEmergencyPhone());
        statement.setString(index, request.getStatus().name());
    }

    static StudentProfileDto read(ResultSet result) throws SQLException {
        Date birthDate = result.getDate("birth_date");
        try {
            return new StudentProfileDto(result.getLong("user_id"),
                    result.getString("display_name"), result.getString("account"),
                    result.getString("student_no"), result.getString("college"),
                    result.getString("major"), result.getString("class_name"),
                    integer(result, "enrollment_year"), integer(result, "expected_graduation_year"),
                    result.getString("degree_level"), result.getString("gender"),
                    JdbcTemporal.localDate(birthDate), result.getString("address"),
                    result.getString("emergency_contact"), result.getString("emergency_phone"),
                    StudentStatus.valueOf(result.getString("status")));
        } catch (IllegalArgumentException ex) {
            throw new SQLException("unknown student status", ex);
        }
    }

    private static int bindLike(PreparedStatement statement, String value, int index)
            throws SQLException {
        if (value != null) statement.setString(index++, "%" + value + "%");
        return index;
    }

    private static Integer integer(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : Integer.valueOf(value);
    }

    private static void setString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.VARCHAR);
        else statement.setString(index, value);
    }

    private static void setInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.INTEGER);
        else statement.setInt(index, value);
    }
}
