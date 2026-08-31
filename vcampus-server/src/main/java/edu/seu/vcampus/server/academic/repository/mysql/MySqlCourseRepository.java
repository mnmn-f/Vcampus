package edu.seu.vcampus.server.academic.repository.mysql;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** courses、排课和课堂资源的仓储门面。 */
final class MySqlCourseRepository {
    private final MySqlCourseQueryRepository queries = new MySqlCourseQueryRepository();
    private final MySqlCourseWriteRepository writes =
            new MySqlCourseWriteRepository(queries);
    private final MySqlScheduleRuleRepository rules = new MySqlScheduleRuleRepository();

    CoursePageDto findCourses(Connection c, CourseQuery q, Long teacherId)
            throws SQLException {
        return queries.findCourses(c, q, teacherId);
    }

    CourseDto findCourse(Connection c, long id, boolean forUpdate) throws SQLException {
        return queries.findCourse(c, id, forUpdate);
    }

    CourseDto saveCourse(Connection c, CourseSaveRequest r, long actor) throws SQLException {
        return writes.saveCourse(c, r, actor);
    }

    CourseScheduleDto saveSchedule(Connection c, ScheduleSaveRequest r) throws SQLException {
        return writes.saveSchedule(c, r);
    }

    CourseScheduleDto findSchedule(Connection c, long id) throws SQLException {
        return queries.findSchedule(c, id);
    }

    boolean deleteSchedule(Connection c, long id) throws SQLException {
        return writes.deleteSchedule(c, id);
    }

    boolean allActiveTeachers(Connection c, List<Long> ids) throws SQLException {
        return rules.allActiveTeachers(c, ids);
    }

    boolean classroomAvailable(Connection c, Long id) throws SQLException {
        return rules.classroomAvailable(c, id);
    }

    boolean hasScheduleConflict(Connection c, ScheduleSaveRequest r) throws SQLException {
        return rules.hasCourseConflict(c, r);
    }

    boolean hasClassroomConflict(Connection c, ScheduleSaveRequest r) throws SQLException {
        return rules.hasClassroomConflict(c, r);
    }
}
