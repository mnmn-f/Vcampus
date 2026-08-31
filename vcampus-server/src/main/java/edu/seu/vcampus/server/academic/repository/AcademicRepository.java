package edu.seu.vcampus.server.academic.repository;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 教务持久化边界。
 *
 * <p>所有写操作都接收服务层建立的连接。MySQL实现因此可以把课程行锁、
 * 选课校验和写入放进同一个 TransactionManager 事务；内存实现忽略连接，
 * 仅用于测试和离线演示。</p>
 */
public interface AcademicRepository {
    CoursePageDto findCourses(Connection connection, CourseQuery query) throws SQLException;

    CoursePageDto findCoursesByTeacher(Connection connection, long teacherUserId,
                                       CourseQuery query) throws SQLException;

    CourseDto findCourse(Connection connection, long courseId) throws SQLException;

    CourseDto lockCourse(Connection connection, long courseId) throws SQLException;

    CourseDto saveCourse(Connection connection, CourseSaveRequest request,
                         long actorUserId) throws SQLException;

    CourseScheduleDto saveSchedule(Connection connection, ScheduleSaveRequest request)
            throws SQLException;

    CourseScheduleDto findSchedule(Connection connection, long scheduleId)
            throws SQLException;

    boolean deleteSchedule(Connection connection, long scheduleId) throws SQLException;

    boolean allActiveTeachers(Connection connection, List<Long> teacherUserIds)
            throws SQLException;

    boolean classroomAvailable(Connection connection, Long classroomId) throws SQLException;

    boolean hasScheduleConflict(Connection connection, ScheduleSaveRequest request)
            throws SQLException;

    boolean hasClassroomConflict(Connection connection, ScheduleSaveRequest request)
            throws SQLException;

    boolean isActiveStudent(Connection connection, long userId) throws SQLException;

    EnrollmentDto findEnrollment(Connection connection, long studentUserId,
                                 long courseId, boolean forUpdate) throws SQLException;

    long countEnrolled(Connection connection, long courseId) throws SQLException;

    boolean hasStudentScheduleConflict(Connection connection, long studentUserId,
                                       long courseId) throws SQLException;

    EnrollmentDto enroll(Connection connection, long studentUserId, long courseId)
            throws SQLException;

    void drop(Connection connection, long studentUserId, long courseId) throws SQLException;

    StudentScheduleDto findStudentSchedule(Connection connection, long studentUserId)
            throws SQLException;
}
