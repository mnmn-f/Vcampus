package edu.seu.vcampus.server.academic;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterRequest;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
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
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

/** 课程花名册的内容与权限边界。 */
public class AcademicCourseRosterServiceTest {
    private AcademicService service;
    private SessionContext student;
    private SessionContext secondStudent;
    private SessionContext academic;
    private SessionContext teacher;
    private SessionContext otherTeacher;
    private SessionContext registrar;

    @Before
    public void setUp() {
        InMemoryAcademicRepository repository = new InMemoryAcademicRepository();
        repository.addActiveStudent(1L, "S001", "学生一", "电气工程学院",
                "电气工程及其自动化", "电气2601");
        repository.addActiveStudent(2L, "S002", "学生二", "计算机科学与工程学院",
                "软件工程", "软工2601");
        repository.addActiveTeacher(10L, "王老师");
        repository.addActiveTeacher(11L, "李老师");
        service = new AcademicService(repository);
        student = session(1L, Role.STUDENT);
        secondStudent = session(2L, Role.STUDENT);
        academic = session(90L, Role.ACADEMIC_ADMIN);
        teacher = session(10L, Role.TEACHER);
        otherTeacher = session(11L, Role.TEACHER);
        registrar = session(91L, Role.REGISTRAR);
    }

    @Test
    public void containsOnlyCurrentlyEnrolledStudents() throws Exception {
        CourseDto course = createCourse("R-001", "花名册课程");
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
    public void emptyCourseReturnsAnEmptyList() throws Exception {
        CourseDto course = createCourse("R-002", "空课程");
        assertEquals(0, service.courseRoster(teacher,
                new CourseRosterRequest(course.getId())).getEntries().size());
    }

    @Test
    public void rejectsOtherTeacherAndNonTeacherRoles() throws Exception {
        final CourseDto course = createCourse("R-003", "权限课程");
        assertForbidden(otherTeacher, course.getId());
        assertForbidden(registrar, course.getId());
        assertForbidden(academic, course.getId());
    }

    @Test
    public void reportsMissingCourse() throws Exception {
        assertCode(AcademicCommands.COURSE_NOT_FOUND, new Operation() {
            @Override public void run() throws Exception {
                service.courseRoster(teacher, new CourseRosterRequest(999999L));
            }
        });
    }

    private CourseDto createCourse(String code, String name) throws Exception {
        CourseSaveRequest request = CourseSaveRequest.create(code, name, CourseType.ELECTIVE,
                BigDecimal.ONE, 16, 10, "测试课程", CourseStatus.PUBLISHED,
                Collections.singletonList(10L));
        return service.createCourse(academic, request);
    }

    private void assertForbidden(final SessionContext session, final long courseId)
            throws Exception {
        assertCode(ResultCodes.FORBIDDEN, new Operation() {
            @Override public void run() throws Exception {
                service.courseRoster(session, new CourseRosterRequest(courseId));
            }
        });
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
