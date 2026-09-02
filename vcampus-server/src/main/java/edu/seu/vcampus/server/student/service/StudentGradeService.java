package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportQuery;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;
import edu.seu.vcampus.common.dto.student.StudentGradeReportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.EnrollmentRecord;
import edu.seu.vcampus.server.student.repository.StudentRecordRepository;

import java.math.BigDecimal;
import java.sql.Connection;

/** 学生查分、任课教师登记和学籍管理员核对成绩服务。 */
public final class StudentGradeService {
    private final StudentRecordRepository repository;
    private final StudentTransactionRunner transactions;
    private final StudentGradeInsightService insights;

    public StudentGradeService(StudentRecordRepository repository,
                               TransactionManager transactionManager) {
        this(repository, new TransactionManagerRunner(transactionManager));
    }

    public StudentGradeService(StudentRecordRepository repository,
                               StudentTransactionRunner transactions) {
        if (repository == null || transactions == null) {
            throw new IllegalArgumentException("grade service dependencies are required");
        }
        this.repository = repository;
        this.transactions = transactions;
        this.insights = new StudentGradeInsightService(repository, transactions);
    }

    public StudentGradePage getOwnGrades(final SessionContext session,
                                          final StudentGradeQuery query)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.SCORE_SELF_READ);
        final StudentGradeQuery safe = query == null ? StudentGradeQuery.firstPage() : query;
        validateStudentQuery(safe);
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentGradePage>() {
                    @Override
                    public StudentGradePage execute(Connection connection)
                            throws StudentRecordException {
                        if (!repository.profileExists(connection, session.getUserId())) {
                            throw new StudentRecordException(ResultCodes.NOT_FOUND,
                                    "学生学籍档案不存在");
                        }
                        return repository.findGrades(connection, session.getUserId(), safe);
                    }
                });
    }

    public StudentGradeReportDto getOwnGradeReport(SessionContext session,
                                                    StudentGradeQuery query)
            throws StudentRecordException {
        return insights.report(session, query);
    }

    public StudentGradeExportDto exportOwnGrades(SessionContext session,
                                                 StudentGradeExportQuery query)
            throws StudentRecordException {
        return insights.export(session, query);
    }

    public StudentGradeDto record(final SessionContext session,
                                  final StudentGradeRecordRequest request)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.SCORE_RECORD);
        if (session.getActiveRole() != Role.TEACHER) {
            throw new StudentRecordException(ResultCodes.FORBIDDEN,
                    "只有任课教师可以登记课程成绩");
        }
        validateRequest(request);
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentGradeDto>() {
                    @Override
                    public StudentGradeDto execute(Connection connection)
                            throws StudentRecordException {
                        EnrollmentRecord enrollment = findEnrollment(connection,
                                request.getEnrollmentId());
                        StudentProfileDto profile = repository.findProfile(connection,
                                enrollment.getStudentUserId());
                        if (profile == null) {
                            throw new StudentRecordException(ResultCodes.NOT_FOUND,
                                    "选课学生的学籍档案不存在");
                        }
                        if (profile.getStatus() == StudentStatus.WITHDRAWN) {
                            throw new StudentRecordException(ResultCodes.CONFLICT,
                                    "已退学学生不能登记成绩");
                        }
                        if (!repository.teacherOwnsCourse(connection, session.getUserId(),
                                enrollment.getCourseId())) {
                            throw new StudentRecordException(ResultCodes.FORBIDDEN,
                                    "只能登记本人授课课程的成绩");
                        }
                        if ("DROPPED".equals(enrollment.getStatus())) {
                            throw new StudentRecordException(ResultCodes.CONFLICT,
                                    "退课记录不能登记成绩");
                        }
                        repository.upsertGrade(connection, session.getUserId(), request);
                        StudentGradeDto grade = repository.findGradeByEnrollment(
                                connection, request.getEnrollmentId());
                        if (grade == null) {
                            throw new StudentRecordException(ResultCodes.INTERNAL_ERROR,
                                    "成绩保存后未找到记录");
                        }
                        return grade;
                    }
                });
    }

    public StudentGradePage review(final SessionContext session,
                                   final StudentGradeReviewQuery query)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.SCORE_RECORD);
        if (session.getActiveRole() != Role.REGISTRAR) {
            throw new StudentRecordException(ResultCodes.FORBIDDEN,
                    "只有学籍管理员可以核对成绩档案");
        }
        final StudentGradeReviewQuery safe = query == null
                ? StudentGradeReviewQuery.firstPage() : query;
        validateReviewQuery(safe);
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentGradePage>() {
                    @Override
                    public StudentGradePage execute(Connection connection) {
                        return repository.reviewGrades(connection, safe);
                    }
                });
    }

    private EnrollmentRecord findEnrollment(Connection connection, long id)
            throws StudentRecordException {
        EnrollmentRecord found = repository.findEnrollment(connection, id);
        if (found == null) {
            throw new StudentRecordException(ResultCodes.NOT_FOUND, "选课记录不存在");
        }
        return found;
    }

    private static void validateRequest(StudentGradeRecordRequest request)
            throws StudentRecordException {
        if (request == null || request.getEnrollmentId() <= 0) {
            throw invalid("选课记录编号必须为正数");
        }
        BigDecimal score = request.getScore();
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0
                || score.compareTo(new BigDecimal("100")) > 0 || score.scale() > 2) {
            throw invalid("成绩必须在0到100之间");
        }
        StudentServiceSupport.maxLength(request.getRemark(), 500, "成绩备注");
    }

    private static void validateReviewQuery(StudentGradeReviewQuery query)
            throws StudentRecordException {
        if (query.getStudentUserId() != null && query.getStudentUserId() <= 0) {
            throw invalid("学生编号必须为正数");
        }
        if (query.getCourseId() != null && query.getCourseId() <= 0) {
            throw invalid("课程编号必须为正数");
        }
    }

    private static void validateStudentQuery(StudentGradeQuery query)
            throws StudentRecordException {
        if (query.getCourseId() != null && query.getCourseId() <= 0) {
            throw invalid("课程编号必须为正数");
        }
        if (query.getSemesterCode() != null && query.getSemesterCode().length() > 32) {
            throw invalid("学期编号长度不能超过32个字符");
        }
    }

    private static StudentRecordException invalid(String message) {
        return new StudentRecordException(ResultCodes.INVALID_INPUT, message);
    }
}
