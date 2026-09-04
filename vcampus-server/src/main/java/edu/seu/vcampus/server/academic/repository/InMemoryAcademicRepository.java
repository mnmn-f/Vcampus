package edu.seu.vcampus.server.academic.repository;

import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentListDto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** 测试和离线演示仓储；不读写磁盘，服务层负责外层原子调用。 */
public final class InMemoryAcademicRepository implements AcademicRepository {
    private final InMemoryAcademicState state = new InMemoryAcademicState();
    private final InMemoryAcademicMapper mapper = new InMemoryAcademicMapper(state);
    private final InMemoryCourseStore courseStore = new InMemoryCourseStore(state, mapper);
    private final InMemoryEnrollmentStore enrollmentStore =
            new InMemoryEnrollmentStore(state, mapper, courseStore);

    public synchronized void addActiveStudent(long userId) {
        addActiveStudent(userId, "S" + userId, "学生" + userId,
                null, null, null);
    }

    public synchronized void addActiveStudent(long userId, String studentNo,
                                              String displayName, String college,
                                              String major, String className) {
        state.students.add(userId);
        state.studentProfiles.put(userId, new InMemoryAcademicState.StudentState(userId,
                studentNo, displayName, college, major, className));
    }

    public synchronized void addActiveTeacher(long userId) {
        addActiveTeacher(userId, "教师" + userId);
    }

    public synchronized void addActiveTeacher(long userId, String displayName) {
        state.teachers.put(userId, displayName == null ? "教师" + userId : displayName);
    }

    public synchronized void addClassroom(ClassroomDto classroom) {
        if (classroom == null || classroom.getId() <= 0) {
            throw new IllegalArgumentException("valid classroom is required");
        }
        state.classrooms.put(classroom.getId(), classroom);
    }

    @Override
    public synchronized CoursePageDto findCourses(Connection c, CourseQuery q) {
        return courseStore.findCourses(q, null);
    }

    @Override
    public synchronized CoursePageDto findCoursesByTeacher(Connection c, long teacherId,
                                                            CourseQuery q) {
        return courseStore.findCourses(q, Long.valueOf(teacherId));
    }

    @Override
    public synchronized boolean teacherOwnsCourse(Connection c, long teacherId,
                                                  long courseId) {
        CourseDto course = courseStore.findCourse(courseId);
        if (course == null) return false;
        for (edu.seu.vcampus.common.dto.academic.CourseInstructorDto instructor
                : course.getInstructors()) {
            if (instructor.getTeacherUserId() == teacherId) return true;
        }
        return false;
    }

    @Override
    public synchronized CourseRosterDto findCourseRoster(Connection c, long teacherId,
                                                         long courseId) {
        if (!teacherOwnsCourse(c, teacherId, courseId)) {
            return new CourseRosterDto(courseId,
                    java.util.Collections.<edu.seu.vcampus.common.dto.academic.CourseRosterEntryDto>emptyList());
        }
        return enrollmentStore.roster(courseId);
    }

    @Override
    public synchronized CourseDto findCourse(Connection c, long id) {
        return courseStore.findCourse(id);
    }

    @Override
    public synchronized CourseDto lockCourse(Connection c, long id) {
        return courseStore.findCourse(id);
    }

    @Override
    public synchronized CourseDto saveCourse(Connection c, CourseSaveRequest request,
                                             long actor) throws SQLException {
        return courseStore.saveCourse(request);
    }

    @Override
    public synchronized CourseScheduleDto saveSchedule(Connection c,
                                                       ScheduleSaveRequest request)
            throws SQLException {
        return courseStore.saveSchedule(request);
    }

    @Override
    public synchronized CourseScheduleDto findSchedule(Connection c, long scheduleId) {
        return courseStore.findSchedule(scheduleId);
    }

    @Override
    public synchronized boolean deleteSchedule(Connection c, long scheduleId) {
        return courseStore.deleteSchedule(scheduleId);
    }

    @Override
    public synchronized boolean allActiveTeachers(Connection c, List<Long> ids) {
        return courseStore.allActiveTeachers(ids);
    }

    @Override
    public synchronized boolean classroomAvailable(Connection c, Long id) {
        return courseStore.classroomAvailable(id);
    }

    @Override
    public synchronized boolean hasScheduleConflict(Connection c, ScheduleSaveRequest r) {
        return courseStore.hasScheduleConflict(r);
    }

    @Override
    public synchronized boolean hasClassroomConflict(Connection c, ScheduleSaveRequest r) {
        return courseStore.hasClassroomConflict(r);
    }

    @Override
    public synchronized boolean isActiveStudent(Connection c, long id) {
        return enrollmentStore.isActiveStudent(id);
    }

    @Override
    public synchronized EnrollmentDto findEnrollment(Connection c, long studentId,
                                                      long courseId, boolean forUpdate) {
        return enrollmentStore.find(studentId, courseId);
    }

    @Override
    public synchronized long countEnrolled(Connection c, long courseId) {
        return enrollmentStore.countEnrolled(courseId);
    }

    @Override
    public synchronized boolean hasStudentScheduleConflict(Connection c, long studentId,
                                                            long courseId) {
        return enrollmentStore.hasScheduleConflict(studentId, courseId);
    }

    @Override
    public synchronized EnrollmentDto enroll(Connection c, long studentId, long courseId) {
        return enrollmentStore.enroll(studentId, courseId);
    }

    @Override
    public synchronized void drop(Connection c, long studentId, long courseId)
            throws SQLException {
        enrollmentStore.drop(studentId, courseId);
    }

    @Override
    public synchronized StudentScheduleDto findStudentSchedule(Connection c, long studentId) {
        return enrollmentStore.schedule(studentId);
    }

    @Override
    public synchronized StudentScheduleDto findStudentSchedule(Connection c, long studentId,
                                                                StudentScheduleQuery query) {
        return enrollmentStore.schedule(studentId, query);
    }

    @Override
    public synchronized StudentEnrollmentListDto findStudentEnrollments(Connection c,
                                                                         long studentId) {
        return enrollmentStore.enrollments(studentId);
    }

    public synchronized int courseCount() {
        return courseStore.count();
    }
}
