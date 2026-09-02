package edu.seu.vcampus.server.academic.repository.mysql;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** courses、course_schedules 和 course_instructors 的写操作。 */
final class MySqlCourseWriteRepository {
    private final MySqlCourseQueryRepository queries;

    MySqlCourseWriteRepository(MySqlCourseQueryRepository queries) {
        this.queries = queries;
    }

    CourseDto saveCourse(Connection c, CourseSaveRequest request, long actor)
            throws SQLException {
        require(c);
        if (request.isUpdate()) {
            updateCourse(c, request);
        } else {
            insertCourse(c, request, actor);
        }
        long id = request.isUpdate() ? request.getCourseId() : lastId(c);
        replaceInstructors(c, id, request.getInstructorUserIds());
        CourseDto saved = queries.findCourse(c, id, false);
        if (saved == null) {
            throw new SQLException("course disappeared after save");
        }
        return saved;
    }

    CourseScheduleDto saveSchedule(Connection c, ScheduleSaveRequest request)
            throws SQLException {
        require(c);
        if (request.isUpdate()) {
            updateSchedule(c, request);
        } else {
            insertSchedule(c, request);
        }
        long id = request.isUpdate() ? request.getScheduleId() : lastId(c);
        return queries.findSchedule(c, id);
    }

    boolean deleteSchedule(Connection c, long id) throws SQLException {
        require(c);
        try (PreparedStatement statement = c.prepareStatement(
                "DELETE FROM course_schedules WHERE id=?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private void insertCourse(Connection c, CourseSaveRequest r, long actor) throws SQLException {
        String sql = "INSERT INTO courses(course_code,course_name,course_type,credits,"
                + "total_hours,capacity,description,status,created_by) VALUES(?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            bindCourse(statement, r, 1);
            statement.setLong(9, actor);
            statement.executeUpdate();
        }
    }

    private void updateCourse(Connection c, CourseSaveRequest r) throws SQLException {
        String sql = "UPDATE courses SET course_code=?,course_name=?,course_type=?,credits=?,"
                + "total_hours=?,capacity=?,description=?,status=? WHERE id=?";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            bindCourse(statement, r, 1);
            statement.setLong(9, r.getCourseId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("course not found");
            }
        }
    }

    private void bindCourse(PreparedStatement statement, CourseSaveRequest r, int index)
            throws SQLException {
        statement.setString(index++, r.getCourseCode());
        statement.setString(index++, r.getCourseName());
        statement.setString(index++, r.getCourseType());
        statement.setBigDecimal(index++, r.getCredits());
        if (r.getTotalHours() == null) {
            statement.setNull(index++, java.sql.Types.SMALLINT);
        } else {
            statement.setInt(index++, r.getTotalHours());
        }
        statement.setInt(index++, r.getCapacity());
        statement.setString(index++, r.getDescription());
        statement.setString(index, r.getStatus());
    }

    private void replaceInstructors(Connection c, long courseId, List<Long> teachers)
            throws SQLException {
        try (PreparedStatement delete = c.prepareStatement(
                "DELETE FROM course_instructors WHERE course_id=?")) {
            delete.setLong(1, courseId);
            delete.executeUpdate();
        }
        if (teachers == null || teachers.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO course_instructors(course_id,teacher_user_id,instructor_role)"
                        + " VALUES(?,?,?)")) {
            for (int i = 0; i < teachers.size(); i++) {
                insert.setLong(1, courseId);
                insert.setLong(2, teachers.get(i));
                insert.setString(3, i == 0 ? "PRIMARY" : "ASSISTANT");
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void insertSchedule(Connection c, ScheduleSaveRequest r) throws SQLException {
        String sql = "INSERT INTO course_schedules(course_id,weekday,start_period,end_period,"
                + "start_date,end_date,classroom_id) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            bindSchedule(statement, r, 1);
            statement.executeUpdate();
        }
    }

    private void updateSchedule(Connection c, ScheduleSaveRequest r) throws SQLException {
        String sql = "UPDATE course_schedules SET course_id=?,weekday=?,start_period=?,"
                + "end_period=?,start_date=?,end_date=?,classroom_id=? WHERE id=?";
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            bindSchedule(statement, r, 1);
            statement.setLong(8, r.getScheduleId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("schedule not found");
            }
        }
    }

    private void bindSchedule(PreparedStatement statement, ScheduleSaveRequest r, int index)
            throws SQLException {
        statement.setLong(index++, r.getCourseId());
        statement.setInt(index++, r.getWeekday());
        statement.setInt(index++, r.getStartPeriod());
        statement.setInt(index++, r.getEndPeriod());
        if (r.getStartDate() == null) {
            statement.setNull(index++, java.sql.Types.DATE);
        } else {
            statement.setDate(index++, JdbcTemporal.date(r.getStartDate()));
        }
        if (r.getEndDate() == null) {
            statement.setNull(index++, java.sql.Types.DATE);
        } else {
            statement.setDate(index++, JdbcTemporal.date(r.getEndDate()));
        }
        if (r.getClassroomId() == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, r.getClassroomId());
        }
    }

    private long lastId(Connection c) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("SELECT LAST_INSERT_ID()")) {
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getLong(1);
                }
            }
        }
        throw new SQLException("failed to read generated id");
    }

    private static void require(Connection c) throws SQLException {
        if (c == null) {
            throw new SQLException("a transaction connection is required");
        }
    }
}
