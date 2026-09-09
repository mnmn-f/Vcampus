package edu.seu.vcampus.server.academic;

import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.repository.InMemoryAcademicRepository;
import edu.seu.vcampus.server.academic.service.AcademicException;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.security.SessionContext;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** 排课写入必须同时保护教师、教室和已选学生的时空约束。 */
public final class AcademicScheduleConflictTest {
    private InMemoryAcademicRepository repository;
    private AcademicService service;
    private SessionContext academic;

    @Before public void setUp() {
        repository = new InMemoryAcademicRepository();
        repository.addActiveStudent(1L, "S1", "学生一", "学院", "专业", "班级");
        repository.addActiveTeacher(10L, "王老师");
        repository.addActiveTeacher(11L, "李老师");
        repository.addClassroom(new ClassroomDto(21L, "楼", "201", "TEACHING", 80, "AVAILABLE"));
        repository.addClassroom(new ClassroomDto(22L, "楼", "202", "TEACHING", 80, "AVAILABLE"));
        repository.addClassroom(new ClassroomDto(23L, "楼", "203", "TEACHING", 20, "AVAILABLE"));
        service = new AcademicService(repository);
        academic = session(90L, Role.ACADEMIC_ADMIN);
    }

    @Test public void multiTeacherCourseConflictsWithAnySharedTeacher() throws Exception {
        CourseDto first = create("MT-1", "联合授课", Arrays.asList(10L, 11L));
        CourseDto second = create("MT-2", "教师二的另一门课", Collections.singletonList(11L));
        service.createSchedule(academic, schedule(first, 2, 1, 2, 21L));
        assertCode(AcademicCommands.SCHEDULE_CONFLICT, new Operation() {
            @Override public void run() throws Exception {
                service.createSchedule(academic, schedule(second, 2, 1, 2, 22L));
            }
        });
    }

    @Test public void updatingCourseCannotOverlapItsAlreadyEnrolledStudent() throws Exception {
        CourseDto first = create("ST-1", "学生已有课", Collections.singletonList(10L));
        CourseDto second = create("ST-2", "学生待改课", Collections.singletonList(11L));
        service.createSchedule(academic, schedule(first, 3, 3, 4, 21L));
        long secondSchedule = service.createSchedule(academic,
                schedule(second, 4, 3, 4, 22L)).getId();
        service.enroll(student(), first.getId());
        service.enroll(student(), second.getId());
        assertCode(AcademicCommands.SCHEDULE_CONFLICT, new Operation() {
            @Override public void run() throws Exception {
                service.updateSchedule(academic, ScheduleSaveRequest.update(secondSchedule,
                        second.getId(), 3, 3, 4, null, null, 22L));
            }
        });
    }

    @Test public void sameTeacherMayUseSamePeriodWhenDateRangesDoNotOverlap() throws Exception {
        CourseDto first = create("DATE-1", "秋季课", Collections.singletonList(10L));
        CourseDto second = create("DATE-2", "春季课", Collections.singletonList(10L));
        service.createSchedule(academic, ScheduleSaveRequest.create(first.getId(), 1, 1, 2,
                org.threeten.bp.LocalDate.of(2026, 9, 1),
                org.threeten.bp.LocalDate.of(2026, 12, 31), 21L));
        service.createSchedule(academic, ScheduleSaveRequest.create(second.getId(), 1, 1, 2,
                org.threeten.bp.LocalDate.of(2027, 2, 1),
                org.threeten.bp.LocalDate.of(2027, 6, 30), 22L));
    }

    @Test public void classroomMustFitCapacityAndRequiredType() throws Exception {
        final CourseDto large = service.createCourse(academic, CourseSaveRequest.create("ROOM-1",
                "大班课", CourseType.ELECTIVE, BigDecimal.ONE, 16, 81, "", CourseStatus.PUBLISHED,
                Collections.singletonList(10L), "2026-FALL"));
        assertCode(AcademicCommands.CLASSROOM_CONFLICT, new Operation() {
            @Override public void run() throws Exception {
                service.createSchedule(academic, schedule(large, 2, 1, 2, 21L));
            }
        });
        final CourseDto lab = service.createCourse(academic, CourseSaveRequest.create("ROOM-2",
                "实验课", CourseType.ELECTIVE, BigDecimal.ONE, 16, 20, "实验室", CourseStatus.PUBLISHED,
                Collections.singletonList(10L), "2026-FALL"));
        assertCode(AcademicCommands.CLASSROOM_CONFLICT, new Operation() {
            @Override public void run() throws Exception {
                service.createSchedule(academic, schedule(lab, 2, 1, 2, 21L));
            }
        });
    }

    @Test public void rejectsInvalidWeekdayAndPeriodBounds() throws Exception {
        final CourseDto course = create("BOUND-1", "边界课程", Collections.singletonList(10L));
        assertCode(AcademicCommands.INVALID_SCHEDULE, new Operation() {
            @Override public void run() throws Exception {
                service.createSchedule(academic, schedule(course, 8, 1, 2, 21L));
            }
        });
        assertCode(AcademicCommands.INVALID_SCHEDULE, new Operation() {
            @Override public void run() throws Exception {
                service.createSchedule(academic, schedule(course, 1, 2, 1, 21L));
            }
        });
    }

    @Test public void scheduleSaveIsFreshForTeacherStudentAndCourseQueries() throws Exception {
        CourseDto course = create("FRESH-1", "同步课", Collections.singletonList(10L));
        service.enroll(student(), course.getId());
        long id = service.createSchedule(academic, schedule(course, 1, 1, 2, 21L)).getId();
        service.updateSchedule(academic, ScheduleSaveRequest.update(id, course.getId(), 5, 6, 7,
                null, null, 22L));
        assertEquals(5, service.studentSchedule(student()).getCourses().get(0).getSchedules()
                .get(0).getWeekday());
        assertEquals(5, service.teacherCourses(session(10L, Role.TEACHER), null).getItems().get(0)
                .getSchedules().get(0).getWeekday());
        assertEquals(5, service.queryCourses(student(), null).getItems().get(0).getSchedules()
                .get(0).getWeekday());
    }

    private CourseDto create(String code, String name, java.util.List<Long> teachers)
            throws Exception {
        return service.createCourse(academic, CourseSaveRequest.create(code, name,
                CourseType.ELECTIVE, BigDecimal.ONE, 16, 80, "", CourseStatus.PUBLISHED,
                teachers, "2026-FALL"));
    }

    private static ScheduleSaveRequest schedule(CourseDto course, int day, int start, int end,
                                                long room) {
        return ScheduleSaveRequest.create(course.getId(), day, start, end, null, null, room);
    }

    private SessionContext student() {
        return session(1L, Role.STUDENT);
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("schedule-" + id + '-' + role.name(), id, "user" + id,
                "用户" + id, EnumSet.of(role), role);
    }

    private static void assertCode(String expected, Operation operation) throws Exception {
        try { operation.run(); } catch (AcademicException ex) {
            assertEquals(expected, ex.getResultCode()); return;
        }
        throw new AssertionError("expected error code " + expected);
    }

    private interface Operation { void run() throws Exception; }
}
