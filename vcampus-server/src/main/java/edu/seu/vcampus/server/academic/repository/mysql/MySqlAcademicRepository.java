package edu.seu.vcampus.server.academic.repository.mysql;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.server.academic.repository.AcademicRepository;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** MySQL教务仓储门面；课程和选课 SQL 分别由两个专责对象维护。 */
public final class MySqlAcademicRepository implements AcademicRepository {
    private final MySqlCourseRepository courseRepository;
    private final MySqlEnrollmentRepository enrollmentRepository;

    public MySqlAcademicRepository() {
        this(new JdbcConnectionFactory());
    }

    public MySqlAcademicRepository(JdbcConnectionFactory connections) {
        if (connections == null) {
            throw new IllegalArgumentException("connections is required");
        }
        this.courseRepository = new MySqlCourseRepository();
        this.enrollmentRepository = new MySqlEnrollmentRepository(courseRepository);
    }

    @Override
    public CoursePageDto findCourses(Connection c, CourseQuery q) throws SQLException {
        return courseRepository.findCourses(c, q, null);
    }

    @Override
    public CoursePageDto findCoursesByTeacher(Connection c, long teacherId,
                                              CourseQuery q) throws SQLException {
        return courseRepository.findCourses(c, q, Long.valueOf(teacherId));
    }

    @Override
    public CourseDto findCourse(Connection c, long id) throws SQLException {
        return courseRepository.findCourse(c, id, false);
    }

    @Override
    public CourseDto lockCourse(Connection c, long id) throws SQLException {
        return courseRepository.findCourse(c, id, true);
    }

    @Override
    public CourseDto saveCourse(Connection c, CourseSaveRequest r, long actor)
            throws SQLException {
        return courseRepository.saveCourse(c, r, actor);
    }

    @Override
    public CourseScheduleDto saveSchedule(Connection c, ScheduleSaveRequest r)
            throws SQLException {
        return courseRepository.saveSchedule(c, r);
    }

    @Override
    public CourseScheduleDto findSchedule(Connection c, long id) throws SQLException {
        return courseRepository.findSchedule(c, id);
    }

    @Override
    public boolean deleteSchedule(Connection c, long id) throws SQLException {
        return courseRepository.deleteSchedule(c, id);
    }

    @Override
    public boolean allActiveTeachers(Connection c, List<Long> ids) throws SQLException {
        return courseRepository.allActiveTeachers(c, ids);
    }

    @Override
    public boolean classroomAvailable(Connection c, Long id) throws SQLException {
        return courseRepository.classroomAvailable(c, id);
    }

    @Override
    public boolean hasScheduleConflict(Connection c, ScheduleSaveRequest r)
            throws SQLException {
        return courseRepository.hasScheduleConflict(c, r);
    }

    @Override
    public boolean hasClassroomConflict(Connection c, ScheduleSaveRequest r)
            throws SQLException {
        return courseRepository.hasClassroomConflict(c, r);
    }

    @Override
    public boolean isActiveStudent(Connection c, long id) throws SQLException {
        return enrollmentRepository.isActiveStudent(c, id);
    }

    @Override
    public EnrollmentDto findEnrollment(Connection c, long studentId, long courseId,
                                        boolean forUpdate) throws SQLException {
        return enrollmentRepository.findEnrollment(c, studentId, courseId, forUpdate);
    }

    @Override
    public long countEnrolled(Connection c, long courseId) throws SQLException {
        return enrollmentRepository.countEnrolled(c, courseId);
    }

    @Override
    public boolean hasStudentScheduleConflict(Connection c, long studentId, long courseId)
            throws SQLException {
        return enrollmentRepository.hasStudentScheduleConflict(c, studentId, courseId);
    }

    @Override
    public EnrollmentDto enroll(Connection c, long studentId, long courseId)
            throws SQLException {
        return enrollmentRepository.enroll(c, studentId, courseId);
    }

    @Override
    public void drop(Connection c, long studentId, long courseId) throws SQLException {
        enrollmentRepository.drop(c, studentId, courseId);
    }

    @Override
    public StudentScheduleDto findStudentSchedule(Connection c, long studentId)
            throws SQLException {
        return enrollmentRepository.findStudentSchedule(c, studentId);
    }

    @Override
    public StudentScheduleDto findStudentSchedule(Connection c, long studentId,
                                                  StudentScheduleQuery query)
            throws SQLException {
        return enrollmentRepository.findStudentSchedule(c, studentId, query);
    }
}
