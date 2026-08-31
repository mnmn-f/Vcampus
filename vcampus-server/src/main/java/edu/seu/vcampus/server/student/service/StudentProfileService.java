package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.dto.student.StudentDetailDto;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.StudentRecordRepository;

import java.sql.Connection;
import org.threeten.bp.LocalDate;
import java.util.List;

/** 学生档案的本人读取与学籍管理员维护服务。 */
public final class StudentProfileService {
    private final StudentRecordRepository repository;
    private final StudentTransactionRunner transactions;

    public StudentProfileService(StudentRecordRepository repository,
                                 TransactionManager transactionManager) {
        this(repository, new TransactionManagerRunner(transactionManager));
    }

    public StudentProfileService(StudentRecordRepository repository,
                                 StudentTransactionRunner transactions) {
        if (repository == null || transactions == null) {
            throw new IllegalArgumentException("student service dependencies are required");
        }
        this.repository = repository;
        this.transactions = transactions;
    }

    public StudentProfileDto getOwnProfile(final SessionContext session)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session,
                Permission.STUDENT_RECORD_SELF_READ);
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentProfileDto>() {
                    @Override
                    public StudentProfileDto execute(Connection connection)
                            throws StudentRecordException {
                        return findProfile(connection, session.getUserId());
                    }
                });
    }

    public StudentProfilePage search(final SessionContext session,
                                     final StudentProfileQuery query)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        final StudentProfileQuery safe = query == null
                ? StudentProfileQuery.firstPage() : query;
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentProfilePage>() {
                    @Override
                    public StudentProfilePage execute(Connection connection) {
                        return repository.searchProfiles(connection, safe);
                    }
                });
    }

    public StudentDetailDto detail(final SessionContext session, final long studentUserId)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        if (studentUserId <= 0) {
            throw invalid("学生编号必须为正数");
        }
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentDetailDto>() {
                    @Override
                    public StudentDetailDto execute(Connection connection)
                            throws StudentRecordException {
                        StudentProfileDto profile = findProfile(connection, studentUserId);
                        List<StudentGradeDto> grades =
                                repository.findAllGrades(connection, studentUserId);
                        return new StudentDetailDto(profile, grades);
                    }
                });
    }

    public StudentProfileDto create(final SessionContext session,
                                    final StudentProfileWriteRequest request)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        validateRequest(request);
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentProfileDto>() {
                    @Override
                    public StudentProfileDto execute(Connection connection)
                            throws StudentRecordException {
                        ensureUserCanReceiveProfile(connection, request);
                        ensureNoStudentNo(connection, request.getStudentNo(), 0L);
                        repository.insertProfile(connection, request);
                        return findProfile(connection, request.getUserId());
                    }
                });
    }

    public StudentProfileDto update(final SessionContext session,
                                    final StudentProfileWriteRequest request)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        validateRequest(request);
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentProfileDto>() {
                    @Override
                    public StudentProfileDto execute(Connection connection)
                            throws StudentRecordException {
                        if (!repository.profileExists(connection, request.getUserId())) {
                            throw new StudentRecordException(ResultCodes.NOT_FOUND,
                                    "学生学籍档案不存在");
                        }
                        ensureNoStudentNo(connection, request.getStudentNo(), request.getUserId());
                        repository.updateProfile(connection, request);
                        return findProfile(connection, request.getUserId());
                    }
                });
    }

    private StudentProfileDto findProfile(Connection connection, long userId)
            throws StudentRecordException {
        StudentProfileDto found = repository.findProfile(connection, userId);
        if (found == null) {
            throw new StudentRecordException(ResultCodes.NOT_FOUND, "学生学籍档案不存在");
        }
        return found;
    }

    private void ensureUserCanReceiveProfile(Connection connection,
                                              StudentProfileWriteRequest request)
            throws StudentRecordException {
        if (!repository.userExists(connection, request.getUserId())) {
            throw new StudentRecordException(ResultCodes.NOT_FOUND, "学生账号不存在");
        }
        if (repository.profileExists(connection, request.getUserId())) {
            throw new StudentRecordException(ResultCodes.CONFLICT, "该账号已有学籍档案");
        }
    }

    private void ensureNoStudentNo(Connection connection, String studentNo,
                                   long excludedUserId) throws StudentRecordException {
        if (repository.studentNoExists(connection, studentNo, excludedUserId)) {
            throw new StudentRecordException(ResultCodes.CONFLICT, "学号已存在");
        }
    }

    private static void validateRequest(StudentProfileWriteRequest request)
            throws StudentRecordException {
        if (request == null || request.getUserId() <= 0) {
            throw invalid("学生账号编号必须为正数");
        }
        StudentServiceSupport.required(request.getStudentNo(), "学号");
        StudentServiceSupport.maxLength(request.getStudentNo(), 32, "学号");
        StudentServiceSupport.maxLength(request.getCollege(), 120, "学院");
        StudentServiceSupport.maxLength(request.getMajor(), 120, "专业");
        StudentServiceSupport.maxLength(request.getClassName(), 120, "班级");
        StudentServiceSupport.maxLength(request.getDegreeLevel(), 20, "学位层次");
        StudentServiceSupport.maxLength(request.getGender(), 16, "性别");
        StudentServiceSupport.maxLength(request.getAddress(), 255, "地址");
        StudentServiceSupport.maxLength(request.getEmergencyContact(), 100, "紧急联系人");
        StudentServiceSupport.maxLength(request.getEmergencyPhone(), 32, "紧急联系电话");
        validateYear(request.getEnrollmentYear(), "入学年份");
        validateYear(request.getExpectedGraduationYear(), "预计毕业年份");
        if (request.getEnrollmentYear() != null && request.getExpectedGraduationYear() != null
                && request.getExpectedGraduationYear() < request.getEnrollmentYear()) {
            throw invalid("预计毕业年份不能早于入学年份");
        }
        if (request.getBirthDate() != null && request.getBirthDate().isAfter(LocalDate.now())) {
            throw invalid("出生日期不能晚于今天");
        }
        if (request.getStatus() == null) {
            throw invalid("请选择学籍状态");
        }
    }

    private static void validateYear(Integer year, String field) throws StudentRecordException {
        if (year != null && (year < 1900 || year > 2200)) {
            throw invalid(field + "不在有效范围内");
        }
    }

    private static StudentRecordException invalid(String message) {
        return new StudentRecordException(ResultCodes.INVALID_INPUT, message);
    }
}
