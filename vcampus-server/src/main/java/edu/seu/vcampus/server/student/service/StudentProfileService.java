package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.dto.student.StudentDetailDto;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidatePage;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidateQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
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
import java.util.Collections;

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

    public StudentAccountCandidatePage pendingAccounts(final SessionContext session,
                                                       final StudentAccountCandidateQuery query)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        final StudentAccountCandidateQuery safe = query == null
                ? StudentAccountCandidateQuery.firstPage() : query;
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentAccountCandidatePage>() {
                    @Override public StudentAccountCandidatePage execute(Connection connection) {
                        return repository.searchPendingAccounts(connection, safe);
                    }
                });
    }

    public StudentDetailDto detail(final SessionContext session, final long studentUserId)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        if (studentUserId <= 0) {
            throw StudentProfileValidation.invalid("学生档案不存在");
        }
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentDetailDto>() {
                    @Override
                    public StudentDetailDto execute(Connection connection)
                            throws StudentRecordException {
                        StudentProfileDto profile = findProfile(connection, studentUserId);
                        return new StudentDetailDto(profile, Collections.emptyList());
                    }
                });
    }

    public StudentProfileDto create(final SessionContext session,
                                    final StudentProfileWriteRequest request)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        StudentProfileValidation.validate(request);
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

    public StudentProfileDto createFromAccount(final SessionContext session,
                                               final StudentProfileCreateRequest request)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        if (request == null || request.getAccount() == null
                || request.getAccount().trim().isEmpty()) {
            throw StudentProfileValidation.invalid("请选择待建档学生账号");
        }
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentProfileDto>() {
                    @Override public StudentProfileDto execute(Connection connection)
                            throws StudentRecordException {
                        long userId = repository.findPendingAccountId(connection,
                                request.getAccount());
                        if (userId <= 0L) {
                            throw new StudentRecordException(ResultCodes.NOT_FOUND,
                                    "学生账号不存在或已有学籍");
                        }
                        StudentProfileWriteRequest write = request.withUserId(userId);
                        StudentProfileValidation.validate(write);
                        ensureNoStudentNo(connection, write.getStudentNo(), 0L);
                        repository.insertProfile(connection, write);
                        return findProfile(connection, userId);
                    }
                });
    }

    public StudentProfileDto update(final SessionContext session,
                                    final StudentProfileWriteRequest request)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        StudentProfileValidation.validate(request);
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

}
