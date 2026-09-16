package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.student.service.StudentAccountProvisioner;
import edu.seu.vcampus.server.student.service.StudentRecordException;
import java.sql.Connection;

/** 复用身份仓储，在学籍事务内原子创建账号和学生角色。 */
public final class IdentityStudentAccountProvisioner implements StudentAccountProvisioner {
    private final IdentityRecordRepository repository;
    private final PasswordHasher hasher;

    public IdentityStudentAccountProvisioner(IdentityRecordRepository repository,
                                              PasswordHasher hasher) {
        if (repository == null || hasher == null) throw new IllegalArgumentException("identity dependencies required");
        this.repository = repository; this.hasher = hasher;
    }

    @Override public long create(Connection connection, SessionContext operator,
                                 StudentProfileCreateRequest request)
            throws StudentRecordException {
        RegistrationRequest registration = new RegistrationRequest(request.getAccount(),
                request.getInitialPassword(), request.getDisplayName());
        try {
            IdentityProfileService.validateRegistration(registration);
            if (repository.findByAccount(connection, registration.getAccount(), true) != null) {
                throw new StudentRecordException(ResultCodes.CONFLICT, "校园账号已存在");
            }
            long id = repository.insertStudent(connection, registration,
                    hasher.hash(registration.getPassword()));
            IdentityServiceSupport.audit(repository, connection, operator,
                    "STUDENT_ACCOUNT_CREATE", "USER", Long.valueOf(id), null);
            return id;
        } catch (IdentityServiceException ex) {
            throw new StudentRecordException(ex.getResultCode(), ex.getUserMessage(), ex);
        } catch (RuntimeException ex) {
            throw new StudentRecordException(ResultCodes.CONFLICT, "学生账号无法创建", ex);
        }
    }
}
