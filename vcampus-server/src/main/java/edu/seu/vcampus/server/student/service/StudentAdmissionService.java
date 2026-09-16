package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.repository.StudentRecordRepository;
import java.sql.Connection;

/** 学籍管理员一次完成学生账号、角色与学籍录入。 */
final class StudentAdmissionService {
    private final StudentRecordRepository repository;
    private final StudentTransactionRunner transactions;
    private final StudentAccountProvisioner accounts;

    StudentAdmissionService(StudentRecordRepository repository,
                            StudentTransactionRunner transactions,
                            StudentAccountProvisioner accounts) {
        this.repository = repository; this.transactions = transactions; this.accounts = accounts;
    }

    StudentProfileDto create(final SessionContext session,
                             final StudentProfileCreateRequest request)
            throws StudentRecordException {
        StudentServiceSupport.requirePermission(session, Permission.STUDENT_RECORD_MANAGE);
        if (accounts == null) {
            throw new StudentRecordException(ResultCodes.INTERNAL_ERROR, "新生账号录入暂不可用");
        }
        return StudentServiceSupport.inTransaction(transactions,
                new TransactionWork<StudentProfileDto>() {
                    @Override public StudentProfileDto execute(Connection connection)
                            throws StudentRecordException {
                        long userId = accounts.create(connection, session, request);
                        StudentProfileWriteRequest write = request.withUserId(userId);
                        StudentProfileValidation.validate(write);
                        if (repository.studentNoExists(connection, write.getStudentNo(), 0L)) {
                            throw new StudentRecordException(ResultCodes.CONFLICT, "学号已存在");
                        }
                        repository.insertProfile(connection, write);
                        StudentProfileDto result = repository.findProfile(connection, userId);
                        if (result == null) {
                            throw new StudentRecordException(ResultCodes.INTERNAL_ERROR, "新生录入后无法读取");
                        }
                        return result;
                    }
                });
    }
}
