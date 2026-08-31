package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.InMemoryStudentRecordRepository;
import edu.seu.vcampus.server.student.service.StudentRecordException;
import edu.seu.vcampus.server.student.service.StudentRecordService;
import edu.seu.vcampus.server.student.service.StudentTransactionRunner;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

/** 成绩范围、教师数据范围、学生隔离和管理员核对测试。 */
public class StudentGradeServiceTest {
    private InMemoryStudentRecordRepository repository;
    private StudentRecordService service;
    private SessionContext teacher;
    private SessionContext otherTeacher;
    private SessionContext student;
    private SessionContext registrar;

    @Before
    public void setUp() throws Exception {
        repository = new InMemoryStudentRecordRepository();
        repository.addUser(1L, "student", "学生");
        repository.addUser(2L, "student2", "学生二");
        repository.addUser(7L, "teacher", "任课教师");
        repository.addUser(8L, "teacher2", "另一教师");
        repository.addUser(9L, "registrar", "学籍管理员");
        service = new StudentRecordService(repository, immediateTransactions());
        service.createProfile(session(9L, Role.REGISTRAR), profile(1L, "S001"));
        service.createProfile(session(9L, Role.REGISTRAR), profile(2L, "S002"));
        repository.addCourse(101L, "CS101", "程序设计");
        repository.addCourse(102L, "CS102", "数据库");
        repository.addEnrollment(1001L, 1L, 101L, "COMPLETED");
        repository.addEnrollment(1002L, 2L, 101L, "COMPLETED");
        repository.addEnrollment(1003L, 1L, 102L, "COMPLETED");
        repository.addInstructor(101L, 7L);
        repository.addInstructor(102L, 8L);
        teacher = session(7L, Role.TEACHER);
        otherTeacher = session(8L, Role.TEACHER);
        student = session(1L, Role.STUDENT);
        registrar = session(9L, Role.REGISTRAR);
    }

    @Test
    public void teacherCanRecordOnlyOwnCourse() throws Exception {
        StudentGradeDto result = service.recordGrade(teacher,
                grade(1001L, "92"));

        assertEquals(new BigDecimal("92"), result.getScore());
        try {
            service.recordGrade(teacher, grade(1003L, "88"));
        } catch (StudentRecordException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
            return;
        }
        throw new AssertionError("teacher should not record another teacher's course");
    }

    @Test
    public void scoreMustBeBetweenZeroAndOneHundred() throws Exception {
        try {
            service.recordGrade(teacher, grade(1001L, "100.01"));
        } catch (StudentRecordException ex) {
            assertEquals(ResultCodes.INVALID_INPUT, ex.getResultCode());
            return;
        }
        throw new AssertionError("out of range score should fail");
    }

    @Test
    public void registrarCanReviewGradesButCannotPretendToBeTeacher() throws Exception {
        service.recordGrade(teacher, grade(1001L, "92"));
        StudentGradePage page = service.reviewGrades(registrar,
                new StudentGradeReviewQuery(1L, 101L, 1, 20));

        assertEquals(1L, page.getTotal());
        try {
            service.reviewGrades(teacher, StudentGradeReviewQuery.firstPage());
        } catch (StudentRecordException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
            return;
        }
        throw new AssertionError("teacher should not review all grade archives");
    }

    @Test
    public void studentReadsOnlyOwnGrades() throws Exception {
        service.recordGrade(teacher, grade(1001L, "92"));
        StudentGradePage page = service.getOwnGrades(student, null);

        assertEquals(1L, page.getTotal());
        assertEquals(1L, page.getItems().get(0).getStudentUserId());
    }

    @Test
    public void teacherCannotRecordForUnknownEnrollment() throws Exception {
        try {
            service.recordGrade(otherTeacher, grade(9999L, "80"));
        } catch (StudentRecordException ex) {
            assertEquals(ResultCodes.NOT_FOUND, ex.getResultCode());
            return;
        }
        throw new AssertionError("unknown enrollment should fail");
    }

    private static StudentGradeRecordRequest grade(long enrollmentId, String score) {
        return new StudentGradeRecordRequest(enrollmentId, new BigDecimal(score),
                new BigDecimal("4.00"), "阶段成绩");
    }

    private static StudentProfileWriteRequest profile(long id, String no) {
        return new StudentProfileWriteRequest(id, no, "软件工程", "软件工程",
                "软工2601", 2026, 2030, "UNDERGRADUATE", "UNKNOWN", null,
                null, null, null, StudentStatus.ENROLLED);
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("token-" + id + "-" + role.name(), id,
                "user" + id, "用户" + id, EnumSet.of(role), role);
    }

    private static StudentTransactionRunner immediateTransactions() {
        return new StudentTransactionRunner() {
            @Override
            public <T> T execute(TransactionWork<T> work) throws Exception {
                return work.execute(null);
            }
        };
    }
}
