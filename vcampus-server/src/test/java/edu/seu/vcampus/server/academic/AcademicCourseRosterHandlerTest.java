package edu.seu.vcampus.server.academic;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterRequest;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.handler.AcademicCommandHandler;
import edu.seu.vcampus.server.academic.repository.InMemoryAcademicRepository;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** COURSE_ROSTER 的成功、越权和课程不存在协议响应。 */
public final class AcademicCourseRosterHandlerTest {
    private AcademicService service;
    private AcademicCommandHandler handler;
    private SessionContext teacher;
    private SessionContext otherTeacher;
    private long courseId;

    @Before public void setUp() throws Exception {
        InMemoryAcademicRepository repository = new InMemoryAcademicRepository();
        repository.addActiveStudent(1L, "S001", "学生一", "学院", "专业", "班级");
        repository.addActiveTeacher(10L, "任课教师");
        repository.addActiveTeacher(11L, "其他教师");
        service = new AcademicService(repository);
        teacher = session(10L, Role.TEACHER);
        otherTeacher = session(11L, Role.TEACHER);
        SessionContext academic = session(90L, Role.ACADEMIC_ADMIN);
        SessionContext student = session(1L, Role.STUDENT);
        CourseDto course = service.createCourse(academic, CourseSaveRequest.create(
                "R-H-001", "协议花名册", CourseType.ELECTIVE, BigDecimal.ONE,
                16, 10, "", CourseStatus.PUBLISHED, Collections.singletonList(10L)));
        courseId = course.getId(); service.enroll(student, courseId);
        handler = new AcademicCommandHandler(AcademicCommands.COURSE_ROSTER, service);
    }

    @Test public void rosterCommandReturnsDto() {
        Message response = handler.handle(request(courseId), teacher);
        assertTrue(response.isSuccess());
        CourseRosterDto roster = (CourseRosterDto) response.getPayload();
        assertEquals(1, roster.getEntries().size());
        assertEquals("S001", roster.getEntries().get(0).getStudentNo());
    }

    @Test public void rosterCommandRejectsNonInstructor() {
        Message response = handler.handle(request(courseId), otherTeacher);
        assertEquals(ResultCodes.FORBIDDEN, response.getResultCode());
    }

    @Test public void rosterCommandReportsMissingCourse() {
        Message response = handler.handle(request(999999L), teacher);
        assertEquals(AcademicCommands.COURSE_NOT_FOUND, response.getResultCode());
    }

    private static Message request(long id) {
        return Message.request(AcademicCommands.COURSE_ROSTER, "token",
                new CourseRosterRequest(id));
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("token-" + id, id, "user" + id, "用户" + id,
                EnumSet.of(role), role);
    }
}
