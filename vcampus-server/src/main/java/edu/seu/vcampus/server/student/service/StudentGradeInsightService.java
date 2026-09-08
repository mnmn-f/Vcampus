package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeMetricsDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeReportDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.StudentRecordRepository;

import java.sql.Connection;
import java.util.List;

/** 学生成绩指标和导出服务，复用成绩仓储及同一事务边界。 */
final class StudentGradeInsightService {
    private final StudentRecordRepository repository;
    private final StudentTransactionRunner transactions;

    StudentGradeInsightService(StudentRecordRepository repository,
                                StudentTransactionRunner transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    StudentGradeReportDto report(final SessionContext session, final StudentGradeQuery query)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.SCORE_SELF_READ);
        final StudentGradeQuery safe = query == null ? StudentGradeQuery.firstPage() : query;
        validate(safe.getCourseId(), safe.getSemesterCode());
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentGradeReportDto>() {
                    @Override public StudentGradeReportDto execute(Connection connection)
                            throws StudentRecordException {
                        requireProfile(connection, session.getUserId());
                        StudentGradePage page = repository.findGrades(connection,
                                session.getUserId(), safe);
                        List<StudentGradeDto> all = repository.findAllGrades(connection,
                                session.getUserId(), safe.getSemesterCode(),
                                safe.getCourseId(), 0);
                        StudentGradeMetricsDto metrics = StudentGradeMetricsCalculator
                                .calculate(safe.getSemesterCode(), all);
                        return new StudentGradeReportDto(page, metrics);
                    }
                });
    }

    StudentGradeExportDto export(final SessionContext session,
                                 final StudentGradeExportQuery query)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.SCORE_SELF_READ);
        final StudentGradeExportQuery safe = query == null
                ? new StudentGradeExportQuery(null, null) : query;
        validate(safe.getCourseId(), safe.getSemesterCode());
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentGradeExportDto>() {
                    @Override public StudentGradeExportDto execute(Connection connection)
                            throws StudentRecordException {
                        requireProfile(connection, session.getUserId());
                        List<StudentGradeDto> rows = repository.findAllGrades(connection,
                                session.getUserId(), safe.getSemesterCode(),
                                safe.getCourseId(), StudentGradeExportQuery.MAX_ROWS + 1);
                        if (rows.size() > StudentGradeExportQuery.MAX_ROWS) {
                            throw new StudentRecordException(StudentCommands.GRADE_EXPORT_TOO_LARGE,
                                    "成绩记录超过单次导出上限5000条");
                        }
                        return new StudentGradeExportDto(rows,
                                StudentGradeMetricsCalculator.calculate(safe.getSemesterCode(), rows));
                    }
                });
    }

    private void requireProfile(Connection connection, long userId)
            throws StudentRecordException {
        if (!repository.profileExists(connection, userId)) {
            throw new StudentRecordException(ResultCodes.NOT_FOUND, "学生学籍档案不存在");
        }
    }

    private static void validate(Long courseId, String semesterCode)
            throws StudentRecordException {
        if (courseId != null && courseId <= 0) throw invalid("课程编号必须为正数");
        if (semesterCode != null && semesterCode.length() > 32) {
            throw invalid("学期编号长度不能超过32个字符");
        }
    }

    private static StudentRecordException invalid(String message) {
        return new StudentRecordException(ResultCodes.INVALID_INPUT, message);
    }
}
