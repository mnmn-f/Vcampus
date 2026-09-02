package edu.seu.vcampus.server.academic;

import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.repository.InMemoryAcademicRepository;
import edu.seu.vcampus.server.academic.service.AcademicException;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import org.threeten.bp.LocalDate;
import java.util.EnumSet;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** 以真实业务规则覆盖内存仓储；MySQL仓储沿用相同服务契约。 */
public class AcademicServiceTest {
    private InMemoryAcademicRepository repository;
    private AcademicService service;
    private SessionContext student;
    private SessionContext secondStudent;
    private SessionContext academic;
    private SessionContext teacher;
    private SessionContext otherTeacher;
    private SessionContext registrar;

    @Before
    public void setUp() {
        repository = new InMemoryAcademicRepository();
        repository.addActiveStudent(1L, "S001", "学生一", "电气工程学院",
                "电气工程及其自动化", "电气2601");
        repository.addActiveStudent(2L, "S002", "学生二", "计算机科学与工程学院",
                "软件工程", "软工2601");
        repository.addActiveTeacher(10L, "王老师");
        repository.addActiveTeacher(11L, "李老师");
        repository.addClassroom(new ClassroomDto(21L, "九龙湖教学楼", "B201",
                "TEACHING", 80, "AVAILABLE"));
        service = new AcademicService(repository);
        student = session(1L, Role.STUDENT);
        secondStudent = session(2L, Role.STUDENT);
        academic = session(90L, Role.ACADEMIC_ADMIN);
        teacher = session(10L, Role.TEACHER);
        otherTeacher = session(11L, Role.TEACHER);
        registrar = session(91L, Role.REGISTRAR);
    }

    @Test
    public void academicTeacherCreatesCourseAndOwnCourseQueryWorks() throws Exception {
        CourseDto course = service.createCourse(academic, course("A-001", "数据库原理", 2,
                CourseStatus.PUBLISHED, 10L));
        assertEquals("A-001", course.getCourseCode());
        service.createSchedule(academic, ScheduleSaveRequest.create(course.getId(), 2, 1, 2,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), 21L));

        CoursePageDto page = service.teacherCourses(teacher, null);
        assertEquals(1L, page.getTotalElements());
        assertEquals(course.getId(), page.getItems().get(0).getId());
        assertEquals(1, page.getItems().get(0).getSchedules().size());
    }

    @Test
    public void enrollmentChecksDuplicateCapacityAndDrop() throws Exception {
        final CourseDto course = service.createCourse(academic, course("A-002", "编译原理", 1,
                CourseStatus.PUBLISHED, 10L));
        EnrollmentDto enrolled = service.enroll(student, course.getId());
        assertEquals(1L, enrolled.getStudentUserId());
        assertCode(AcademicCommands.DUPLICATE_ENROLLMENT, new Operation() {
            @Override
            public void run() throws Exception {
                service.enroll(student, course.getId());
            }
        });
        assertCode(AcademicCommands.COURSE_CAPACITY_FULL, new Operation() {
            @Override
            public void run() throws Exception {
                service.enroll(secondStudent, course.getId());
            }
        });
        service.drop(student, course.getId());
        assertEquals(0L, repository.countEnrolled(null, course.getId()));
    }

    @Test
    public void enrollmentRejectsOverlappingClassPeriods() throws Exception {
        CourseDto first = service.createCourse(academic, course("A-003", "操作系统", 10,
                CourseStatus.PUBLISHED, 10L));
        final CourseDto second = service.createCourse(academic, course("A-004", "计算机网络", 10,
                CourseStatus.PUBLISHED, 10L));
        service.createSchedule(academic, ScheduleSaveRequest.create(first.getId(), 3, 3, 4,
                null, null, null));
        service.createSchedule(academic, ScheduleSaveRequest.create(second.getId(), 3, 4, 5,
                null, null, null));
        service.enroll(student, first.getId());
        assertCode(AcademicCommands.SCHEDULE_CONFLICT, new Operation() {
            @Override
            public void run() throws Exception {
                service.enroll(student, second.getId());
            }
        });
    }

    @Test
    public void classroomAndRoleBoundariesAreEnforced() throws Exception {
        CourseDto first = service.createCourse(academic, course("A-005", "软件测试", 10,
                CourseStatus.PUBLISHED, 10L));
        final CourseDto second = service.createCourse(academic, course("A-006", "软件质量", 10,
                CourseStatus.PUBLISHED, 10L));
        service.createSchedule(academic, ScheduleSaveRequest.create(first.getId(), 4, 1, 2,
                null, null, 21L));
        assertCode(AcademicCommands.CLASSROOM_CONFLICT, new Operation() {
            @Override
            public void run() throws Exception {
                service.createSchedule(academic, ScheduleSaveRequest.create(second.getId(), 4,
                        2, 3, null, null, 21L));
            }
        });
        assertCode(ResultCodes.FORBIDDEN, new Operation() {
            @Override
            public void run() throws Exception {
                service.createCourse(student, course("A-007", "越权课程", 10,
                        CourseStatus.DRAFT, 10L));
            }
        });
    }

    @Test
    public void teacherOnlySeesCoursesAssignedToCurrentUser() throws Exception {
        CourseDto mine = service.createCourse(academic, course("A-008", "我的课程", 10,
                CourseStatus.DRAFT, 10L));
        service.createCourse(academic, course("A-009", "其他课程", 10,
                CourseStatus.DRAFT, 11L));
        assertEquals(1L, service.teacherCourses(teacher, null).getTotalElements());
        assertEquals(mine.getId(), service.teacherCourses(teacher, null).getItems().get(0).getId());
    }

    @Test
    public void courseQueriesArePagedAndStudentsOnlySeePublishedRows() throws Exception {
        service.createCourse(academic, course("A-010", "已发布一", 10,
                CourseStatus.PUBLISHED, 10L));
        service.createCourse(academic, course("A-011", "草稿", 10,
                CourseStatus.DRAFT, 10L));
        service.createCourse(academic, course("A-012", "已发布二", 10,
                CourseStatus.PUBLISHED, 10L));
        CoursePageDto first = service.queryCourses(academic,
                new CourseQuery(1, 1, null, null, null));
        assertEquals(3L, first.getTotalElements());
        assertEquals(3, first.getTotalPages());
        assertEquals(1, first.getItems().size());
        assertEquals("A-012", first.getItems().get(0).getCourseCode());
        assertEquals(2L, service.queryCourses(student, null).getTotalElements());
    }

    @Test
    public void courseRosterContainsOnlyCurrentlyEnrolledStudents() throws Exception {
        CourseDto course = service.createCourse(academic, course("R-001", "花名册课程", 10,
                CourseStatus.PUBLISHED, 10L));
        EnrollmentDto first = service.enroll(student, course.getId());
        service.enroll(secondStudent, course.getId());
        service.drop(secondStudent, course.getId());

        CourseRosterDto roster = service.courseRoster(teacher,
                new CourseRosterRequest(course.getId()));

        assertEquals(course.getId(), roster.getCourseId());
        assertEquals(1, roster.getEntries().size());
        assertEquals(first.getId(), roster.getEntries().get(0).getEnrollmentId());
        assertEquals("S001", roster.getEntries().get(0).getStudentNo());
        assertEquals("学生一", roster.getEntries().get(0).getDisplayName());
        assertEquals("ENROLLED", roster.getEntries().get(0).getEnrollmentStatus());
    }

    @Test
    public void emptyCourseRosterReturnsAnEmptyList() throws Exception {
        CourseDto course = service.createCourse(academic, course("R-002", "空课程", 10,
                CourseStatus.PUBLISHED, 10L));
        assertEquals(0, service.courseRoster(teacher,
                new CourseRosterRequest(course.getId())).getEntries().size());
    }

    @Test
    public void rosterRejectsOtherTeacherAndNonTeacherRoles() throws Exception {
        final CourseDto course = service.createCourse(academic, course("R-003", "权限课程", 10,
                CourseStatus.PUBLISHED, 10L));
        assertCode(ResultCodes.FORBIDDEN, new Operation() {
            @Override public void run() throws Exception {
                service.courseRoster(otherTeacher, new CourseRosterRequest(course.getId()));
            }
        });
        assertCode(ResultCodes.FORBIDDEN, new Operation() {
            @Override public void run() throws Exception {
                service.courseRoster(registrar, new CourseRosterRequest(course.getId()));
            }
        });
        assertCode(ResultCodes.FORBIDDEN, new Operation() {
            @Override public void run() throws Exception {
                service.courseRoster(academic, new CourseRosterRequest(course.getId()));
            }
        });
    }

    @Test
    public void rosterReportsMissingCourse() throws Exception {
        assertCode(AcademicCommands.COURSE_NOT_FOUND, new Operation() {
            @Override public void run() throws Exception {
                service.courseRoster(teacher, new CourseRosterRequest(999999L));
            }
        });
    }

    private static CourseSaveRequest course(String code, String name, int capacity,
                                            CourseStatus status, long teacherId) {
        return CourseSaveRequest.create(code, name, CourseType.ELECTIVE,
                BigDecimal.ONE, 16, capacity, "测试课程", status,
                Collections.singletonList(teacherId));
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("token-" + id + '-' + role.name(), id, "user" + id,
                "用户" + id, EnumSet.of(role), role);
    }

    private static void assertCode(String expected, Operation operation) throws Exception {
        try {
            operation.run();
        } catch (AcademicException ex) {
            assertEquals(expected, ex.getResultCode());
            return;
        }
        throw new AssertionError("expected error code " + expected);
    }

    private interface Operation {
        void run() throws Exception;
    }
}
