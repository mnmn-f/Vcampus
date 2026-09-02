package edu.seu.vcampus.server.academic;

import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.academic.repository.InMemoryAcademicRepository;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

/** 学生课表按学期和会话身份查询的契约。 */
public final class AcademicScheduleServiceTest {
    private AcademicService service;
    private SessionContext academic;
    private SessionContext student;
    private SessionContext secondStudent;

    @Before
    public void setUp() {
        InMemoryAcademicRepository repository = new InMemoryAcademicRepository();
        repository.addActiveStudent(1L);
        repository.addActiveStudent(2L);
        repository.addActiveTeacher(10L, "王老师");
        service = new AcademicService(repository);
        academic = session(90L, Role.ACADEMIC_ADMIN);
        student = session(1L, Role.STUDENT);
        secondStudent = session(2L, Role.STUDENT);
    }

    @Test
    public void filtersByTermAndUsesSessionStudentInsteadOfRequestId() throws Exception {
        CourseDto fall = service.createCourse(academic, course("A-013", "秋季课", "2026-FALL"));
        CourseDto spring = service.createCourse(academic,
                course("A-014", "春季课", "2027-SPRING"));
        service.enroll(student, fall.getId());
        service.enroll(student, spring.getId());

        assertEquals(1, service.studentSchedule(student,
                new StudentScheduleQuery("2026-FALL")).getCourses().size());
        assertEquals(2, service.studentSchedule(student,
                StudentScheduleQuery.all()).getCourses().size());
        assertEquals(0, service.studentSchedule(secondStudent,
                StudentScheduleQuery.all()).getCourses().size());
    }

    private static CourseSaveRequest course(String code, String name, String term) {
        return CourseSaveRequest.create(code, name, CourseType.ELECTIVE, BigDecimal.ONE,
                16, 10, "测试课程", CourseStatus.PUBLISHED,
                Collections.singletonList(10L), term);
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("schedule-token-" + id, id, "user" + id,
                "用户" + id, EnumSet.of(role), role);
    }
}
