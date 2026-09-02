package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeMetricsDto;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeReportDto;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.InMemoryStudentRecordRepository;
import edu.seu.vcampus.server.student.service.StudentRecordService;
import edu.seu.vcampus.server.student.service.StudentTransactionRunner;
import edu.seu.vcampus.server.db.TransactionWork;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** 成绩指标的学期筛选、可信计入标记和本人导出范围测试。 */
public final class StudentGradeInsightServiceTest {
    private InMemoryStudentRecordRepository repository;
    private StudentRecordService service;
    private SessionContext student;

    @Before
    public void setUp() throws Exception {
        repository = new InMemoryStudentRecordRepository();
        repository.addUser(1L, "student", "学生");
        repository.addUser(2L, "other", "其他");
        repository.addUser(9L, "registrar", "管理员");
        service = new StudentRecordService(repository, immediateTransactions());
        service.createProfile(session(9L, Role.REGISTRAR), profile(1L, "S001"));
        repository.addCourse(101L, "CS101", "程序设计");
        repository.addCourse(102L, "CS102", "数据库");
        repository.addCourse(103L, "CS103", "实践");
        repository.addEnrollment(1001L, 1L, 101L, "COMPLETED");
        repository.addEnrollment(1002L, 1L, 102L, "COMPLETED");
        repository.addEnrollment(1003L, 1L, 103L, "COMPLETED");
        repository.addEnrollment(2001L, 2L, 101L, "COMPLETED");
        student = session(1L, Role.STUDENT);
    }

    @Test
    public void calculatesFourMetricsForSelectedTermAndLeavesExcludedRowsVisible()
            throws Exception {
        repository.addGrade(grade(1L, 1001L, 1L, 101L, "2026-FALL", "3", "95", true));
        repository.addGrade(grade(2L, 1002L, 1L, 102L, "2026-FALL", "2", "80", true));
        repository.addGrade(grade(3L, 1003L, 1L, 103L, "2026-FALL", "1", "100", false));
        repository.addGrade(grade(4L, 2001L, 2L, 101L, "2026-FALL", "3", "60", true));

        StudentGradeReportDto report = service.getOwnGradeReport(student,
                new StudentGradeQuery("2026-FALL", null, 1, 20));
        StudentGradeMetricsDto metrics = report.getMetrics();

        assertEquals(3L, report.getGrades().getTotal());
        assertEquals(3, metrics.getCourseCount());
        assertEquals(2, metrics.getIncludedCourseCount());
        assertEquals(new BigDecimal("5.00"), metrics.getCredits());
        assertEquals(new BigDecimal("3.90"), metrics.getWeightedGpa());
        assertEquals(new BigDecimal("3.75"), metrics.getAverageGpa());
        assertEquals(new BigDecimal("89.00"), metrics.getWeightedAverageScore());
        assertEquals(new BigDecimal("87.50"), metrics.getAverageScore());
    }

    @Test
    public void cumulativeReportUsesAllTermsAndEmptyScoresProduceNullMetrics()
            throws Exception {
        repository.addGrade(grade(1L, 1001L, 1L, 101L, "2025-SPRING", "3", null, true));
        StudentGradeReportDto report = service.getOwnGradeReport(student, null);
        assertEquals(1L, report.getGrades().getTotal());
        assertEquals(0, report.getMetrics().getCourseCount());
        assertNull(report.getMetrics().getAverageGpa());
        assertNull(report.getMetrics().getWeightedAverageScore());
    }

    @Test
    public void exportIsBoundToSessionStudentAndIncludesOnlySelectedTerm()
            throws Exception {
        repository.addGrade(grade(1L, 1001L, 1L, 101L, "2026-FALL", "3", "95", true));
        repository.addGrade(grade(2L, 2001L, 2L, 101L, "2026-FALL", "3", "98", true));
        repository.addGrade(grade(3L, 1002L, 1L, 102L, "2025-SPRING", "2", "80", true));

        StudentGradeExportDto export = service.exportOwnGrades(student,
                new edu.seu.vcampus.common.dto.student.StudentGradeExportQuery("2026-FALL", null));
        assertEquals(1, export.getItems().size());
        assertEquals(1L, export.getItems().get(0).getStudentUserId());
        assertEquals(new BigDecimal("4.50"), export.getMetrics().getWeightedGpa());
    }

    private static StudentGradeDto grade(long id, long enrollmentId, long studentId,
                                         long courseId, String term, String credits,
                                         String score, boolean included) {
        BigDecimal value = score == null ? null : new BigDecimal(score);
        return new StudentGradeDto(id, enrollmentId, studentId, courseId,
                "C" + courseId, "课程" + courseId, value, null, 9L, null,
                null, "COMPLETED", term, new BigDecimal(credits), included);
    }

    private static StudentProfileWriteRequest profile(long id, String no) {
        return new StudentProfileWriteRequest(id, no, "软件工程", "软件工程", "软工",
                2026, 2030, "UNDERGRADUATE", "UNKNOWN", null, null, null, null,
                StudentStatus.ENROLLED);
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("token-" + id, id, "user" + id, "用户" + id,
                EnumSet.of(role), role);
    }

    private static StudentTransactionRunner immediateTransactions() {
        return new StudentTransactionRunner() {
            @Override public <T> T execute(TransactionWork<T> work) throws Exception {
                return work.execute(null);
            }
        };
    }
}
