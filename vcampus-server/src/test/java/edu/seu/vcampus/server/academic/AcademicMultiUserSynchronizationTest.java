package edu.seu.vcampus.server.academic;

import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.repository.InMemoryAcademicRepository;
import edu.seu.vcampus.server.academic.service.AcademicException;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 教务多人会话的读写一致性：学生、教师和教务管理员看到同一份最新事实。 */
public final class AcademicMultiUserSynchronizationTest {
    private InMemoryAcademicRepository repository;
    private AcademicService service;
    private SessionContext student;
    private SessionContext secondStudent;
    private SessionContext thirdStudent;
    private SessionContext academic;
    private SessionContext teacher;
    private SessionContext secondTeacher;

    @Before public void setUp() {
        repository = new InMemoryAcademicRepository();
        repository.addActiveStudent(1L, "S001", "学生一", "学院一", "专业一", "班一");
        repository.addActiveStudent(2L, "S002", "学生二", "学院二", "专业二", "班二");
        repository.addActiveStudent(3L, "S003", "学生三", "学院三", "专业三", "班三");
        repository.addActiveTeacher(10L, "王老师");
        repository.addActiveTeacher(11L, "李老师");
        repository.addClassroom(new ClassroomDto(21L, "九龙湖", "B201", "TEACHING", 80, "AVAILABLE"));
        repository.addClassroom(new ClassroomDto(22L, "九龙湖", "B202", "TEACHING", 80, "AVAILABLE"));
        service = new AcademicService(repository);
        student = session(1L, Role.STUDENT); secondStudent = session(2L, Role.STUDENT);
        thirdStudent = session(3L, Role.STUDENT); academic = session(90L, Role.ACADEMIC_ADMIN);
        teacher = session(10L, Role.TEACHER); secondTeacher = session(11L, Role.TEACHER);
    }

    @Test public void enrollmentIsVisibleToOtherStudentScheduleAndCourseCount() throws Exception {
        CourseDto course = create("SYNC-1", "同步课程", 2, 10L);
        service.createSchedule(academic, ScheduleSaveRequest.create(course.getId(), 2, 3, 4,
                null, null, 21L));
        service.enroll(student, course.getId());
        assertEquals(1L, service.queryCourses(secondStudent, null).getItems().get(0).getEnrolledCount());
        assertEquals(1, service.studentEnrollments(student).getItems().size());
        assertEquals(1, service.studentSchedule(student).getCourses().size());
        service.enroll(secondStudent, course.getId());
        assertEquals(2L, service.queryCourses(student, null).getItems().get(0).getEnrolledCount());
        service.drop(student, course.getId());
        assertEquals(0, service.studentSchedule(student).getCourses().size());
        assertEquals(1, service.studentSchedule(secondStudent).getCourses().size());
    }

    @Test public void teacherReassignmentAndRosterAreFreshForBothTeacherSessions() throws Exception {
        CourseDto course = create("SYNC-2", "教师变更", 10, 10L);
        service.enroll(student, course.getId());
        assertEquals(1L, service.teacherCourses(teacher, null).getTotalElements());
        assertEquals(1, service.courseRoster(teacher,
                new edu.seu.vcampus.common.dto.academic.CourseRosterRequest(course.getId())).getEntries().size());
        CourseSaveRequest update = CourseSaveRequest.update(course.getId(), "SYNC-2", "教师变更后",
                CourseType.ELECTIVE, BigDecimal.ONE, 16, 10, "更新说明", CourseStatus.PUBLISHED,
                Collections.singletonList(11L));
        CourseDto changed = service.updateCourse(academic, update);
        assertEquals("教师变更后", changed.getCourseName());
        assertEquals(0L, service.teacherCourses(teacher, null).getTotalElements());
        assertEquals(1L, service.teacherCourses(secondTeacher, null).getTotalElements());
        assertEquals("李老师", service.teacherCourses(secondTeacher, null).getItems().get(0)
                .getInstructors().get(0).getDisplayName());
        assertEquals("教师变更后", service.queryCourses(student, null).getItems().get(0).getCourseName());
    }

    @Test public void scheduleUpdateIsVisibleInStudentScheduleAfterNextQuery() throws Exception {
        CourseDto course = create("SYNC-3", "排课更新", 10, 10L);
        service.enroll(student, course.getId());
        long scheduleId = service.createSchedule(academic, ScheduleSaveRequest.create(course.getId(),
                1, 1, 2, null, null, 21L)).getId();
        service.updateSchedule(academic, ScheduleSaveRequest.update(scheduleId, course.getId(),
                5, 6, 7, null, null, 22L));
        assertEquals(5, service.studentSchedule(student).getCourses().get(0).getSchedules()
                .get(0).getWeekday());
        assertEquals(22L, service.studentSchedule(student).getCourses().get(0).getSchedules()
                .get(0).getClassroom().getId());
    }

    @Test public void completedEnrollmentCannotBeReenrolledOrDropped() throws Exception {
        CourseDto course = create("SYNC-4", "已完成课程", 10, 10L);
        repository.addEnrollment(700L, 1L, course.getId(), "COMPLETED");
        assertCode(AcademicCommands.ENROLLMENT_COMPLETED, new Operation() {
            @Override public void run() throws Exception { service.enroll(student, course.getId()); }
        });
        assertCode(AcademicCommands.ENROLLMENT_COMPLETED, new Operation() {
            @Override public void run() throws Exception { service.drop(student, course.getId()); }
        });
    }

    @Test public void concurrentStudentsCannotExceedCapacity() throws Exception {
        final CourseDto course = create("SYNC-5", "并发容量", 2, 10L);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            List<Future<Boolean>> values = Arrays.asList(pool.submit(enroll(student, course)),
                    pool.submit(enroll(secondStudent, course)), pool.submit(enroll(thirdStudent, course)));
            int success = 0;
            for (Future<Boolean> value : values) if (value.get().booleanValue()) success++;
            assertEquals(2, success); assertEquals(2L, repository.countEnrolled(null, course.getId()));
        } finally { pool.shutdownNow(); }
    }

    private Callable<Boolean> enroll(final SessionContext session, final CourseDto course) {
        return new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
                try { service.enroll(session, course.getId()); return Boolean.TRUE; }
                catch (AcademicException ex) { assertEquals(AcademicCommands.COURSE_CAPACITY_FULL,
                        ex.getResultCode()); return Boolean.FALSE; }
            }
        };
    }

    private CourseDto create(String code, String name, int capacity, long teacherId) throws Exception {
        return service.createCourse(academic, CourseSaveRequest.create(code, name, CourseType.ELECTIVE,
                BigDecimal.ONE, 16, capacity, "同步测试", CourseStatus.PUBLISHED,
                Collections.singletonList(teacherId), "2026-FALL"));
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("academic-" + id + '-' + role.name(), id, "user" + id,
                "用户" + id, EnumSet.of(role), role);
    }

    private static void assertCode(String expected, Operation operation) throws Exception {
        try { operation.run(); } catch (AcademicException ex) { assertEquals(expected, ex.getResultCode()); return; }
        throw new AssertionError("expected error code " + expected);
    }

    private interface Operation { void run() throws Exception; }
}
