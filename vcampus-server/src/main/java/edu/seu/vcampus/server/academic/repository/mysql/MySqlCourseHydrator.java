package edu.seu.vcampus.server.academic.repository.mysql;

import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseInstructorDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 将 courses 结果集补齐为包含时段和教师的公共 DTO。 */
final class MySqlCourseHydrator {
    CourseDto course(Connection c, ResultSet result) throws SQLException {
        long id = result.getLong("id");
        int hours = result.getInt("total_hours");
        Integer totalHours = result.wasNull() ? null : Integer.valueOf(hours);
        return new CourseDto(id, result.getString("course_code"),
                result.getString("course_name"), result.getString("course_type"),
                result.getBigDecimal("credits"), totalHours,
                result.getInt("capacity"), result.getLong("enrolled_count"),
                result.getString("description"), result.getString("status"),
                schedules(c, id), instructors(c, id), result.getString("semester_code"));
    }

    CourseScheduleDto schedule(ResultSet result) throws SQLException {
        ClassroomDto classroom = result.getObject("room_id") == null ? null
                : new ClassroomDto(result.getLong("room_id"),
                result.getString("building_name"), result.getString("room_no"),
                result.getString("classroom_type"), result.getInt("room_capacity"),
                result.getString("room_status"));
        Date start = result.getDate("start_date");
        Date end = result.getDate("end_date");
        return new CourseScheduleDto(result.getLong("id"), result.getLong("course_id"),
                result.getInt("weekday"), result.getInt("start_period"),
                result.getInt("end_period"), JdbcTemporal.localDate(start),
                JdbcTemporal.localDate(end), classroom);
    }

    private List<CourseScheduleDto> schedules(Connection c, long courseId)
            throws SQLException {
        List<CourseScheduleDto> rows = new ArrayList<CourseScheduleDto>();
        try (PreparedStatement statement = c.prepareStatement(MySqlCourseSql.SCHEDULE_SQL)) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(schedule(result));
                }
            }
        }
        return rows;
    }

    private List<CourseInstructorDto> instructors(Connection c, long courseId)
            throws SQLException {
        List<CourseInstructorDto> rows = new ArrayList<CourseInstructorDto>();
        try (PreparedStatement statement = c.prepareStatement(MySqlCourseSql.INSTRUCTOR_SQL)) {
            statement.setLong(1, courseId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new CourseInstructorDto(result.getLong(1),
                            result.getString(2), result.getString(3), result.getString(4)));
                }
            }
        }
        return rows;
    }
}
