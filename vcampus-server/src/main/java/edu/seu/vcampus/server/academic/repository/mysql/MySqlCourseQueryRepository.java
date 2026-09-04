package edu.seu.vcampus.server.academic.repository.mysql;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** courses 的分页读取和 DTO 组装。 */
final class MySqlCourseQueryRepository {
    private final MySqlCourseHydrator hydrator = new MySqlCourseHydrator();

    CoursePageDto findCourses(Connection c, CourseQuery q, Long teacherId)
            throws SQLException {
        require(c);
        q = q == null ? new CourseQuery() : q;
        String where = where(q, teacherId);
        List<Object> values = parameters(q, teacherId);
        long total = count(c, where, values);
        String sql = "SELECT " + MySqlCourseSql.COURSE_COLUMNS
                + MySqlCourseSql.COURSE_FROM + where
                + " ORDER BY c.id DESC LIMIT ? OFFSET ?";
        List<CourseDto> rows = new ArrayList<CourseDto>();
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            int index = bind(statement, values, 1);
            statement.setInt(index++, q.getPageSize());
            statement.setLong(index, (long) (q.getPageNumber() - 1) * q.getPageSize());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(hydrator.course(c, result));
                }
            }
        }
        return new CoursePageDto(q.getPageNumber(), q.getPageSize(), total, rows);
    }

    CourseDto findCourse(Connection c, long id, boolean forUpdate) throws SQLException {
        require(c);
        String sql = "SELECT " + MySqlCourseSql.COURSE_COLUMNS
                + MySqlCourseSql.COURSE_FROM + "WHERE c.id=?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? hydrator.course(c, result) : null;
            }
        }
    }

    CourseScheduleDto findSchedule(Connection c, long id) throws SQLException {
        require(c);
        String sql = MySqlCourseSql.SCHEDULE_SQL.replace(
                "WHERE s.course_id=?", "WHERE s.id=?");
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? hydrator.schedule(result) : null;
            }
        }
    }

    private long count(Connection c, String where, List<Object> values) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(
                "SELECT COUNT(*)" + MySqlCourseSql.COURSE_FROM + where)) {
            bind(statement, values, 1);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }
    }

    private String where(CourseQuery q, Long teacherId) {
        StringBuilder sql = new StringBuilder("WHERE 1=1");
        if (teacherId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM course_instructors ci "
                    + "WHERE ci.course_id=c.id AND ci.teacher_user_id=? )");
        }
        if (q.getKeyword() != null) {
            sql.append(" AND (c.course_code LIKE ? OR c.course_name LIKE ?)");
        }
        if (q.getCourseCode() != null) sql.append(" AND c.course_code LIKE ?");
        if (q.getCourseName() != null) sql.append(" AND c.course_name LIKE ?");
        if (q.getStatus() != null) {
            sql.append(" AND c.status=?");
        }
        if (q.getCourseType() != null) {
            sql.append(" AND c.course_type=?");
        }
        return sql.toString();
    }

    private List<Object> parameters(CourseQuery q, Long teacherId) {
        List<Object> values = new ArrayList<Object>();
        if (teacherId != null) {
            values.add(teacherId);
        }
        if (q.getKeyword() != null) {
            values.add("%" + q.getKeyword() + "%");
            values.add("%" + q.getKeyword() + "%");
        }
        if (q.getCourseCode() != null) values.add("%" + q.getCourseCode() + "%");
        if (q.getCourseName() != null) values.add("%" + q.getCourseName() + "%");
        if (q.getStatus() != null) {
            values.add(q.getStatus());
        }
        if (q.getCourseType() != null) {
            values.add(q.getCourseType());
        }
        return values;
    }

    private int bind(PreparedStatement statement, List<Object> values, int index)
            throws SQLException {
        for (Object value : values) {
            statement.setObject(index++, value);
        }
        return index;
    }

    private static void require(Connection c) throws SQLException {
        if (c == null) {
            throw new SQLException("a transaction connection is required");
        }
    }
}
