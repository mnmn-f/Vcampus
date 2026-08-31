package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.dto.identity.PasswordResetRequest;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.UserPage;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.dto.identity.UserStatus;
import edu.seu.vcampus.common.dto.identity.UserStatusUpdateRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.IdentityUserRecord;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.sql.Connection;

/** 系统管理员用户分页、状态和密码维护用例。 */
final class IdentityUserAdminService {
    private final IdentityRecordRepository repository;
    private final IdentityTransactionRunner transactions;
    private final PasswordHasher hasher;
    private final SessionManager sessionManager;

    IdentityUserAdminService(IdentityRecordRepository repository,
                             IdentityTransactionRunner transactions, PasswordHasher hasher,
                             SessionManager sessionManager) {
        this.repository = repository;
        this.transactions = transactions;
        this.hasher = hasher;
        this.sessionManager = sessionManager;
    }

    UserPage search(SessionContext session, UserQuery query) throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.USER_MANAGE);
        final UserQuery safe = query == null ? new UserQuery() : query;
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<UserPage>() {
                    @Override public UserPage execute(Connection c) { return repository.search(c, safe); }
                });
    }

    ProfileDto updateStatus(final SessionContext session, final UserStatusUpdateRequest request)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.USER_MANAGE);
        validateStatus(request);
        final String status = request.getStatus().trim().toUpperCase();
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<ProfileDto>() {
                    @Override public ProfileDto execute(Connection c) throws IdentityServiceException {
                        ensureNotSelf(session, request.getUserId());
                        IdentityUserRecord target = user(c, request.getUserId(), true);
                        if (!repository.updateStatus(c, target.getUserId(), status)) {
                            throw IdentityServiceSupport.error(ResultCodes.CONFLICT, "用户状态未更新");
                        }
                        if (status.equals(UserStatus.DISABLED.name())
                                || status.equals(UserStatus.CLOSED.name())) {
                            repository.revokeUserSessions(c, target.getUserId());
                            sessionManager.invalidateUserSessions(target.getUserId());
                        }
                        IdentityServiceSupport.audit(repository, c, session, "USER_STATUS_UPDATE",
                                "USER", Long.valueOf(target.getUserId()),
                                "{\"status\":\"" + status + "\"}");
                        return user(c, target.getUserId(), false).toProfile();
                    }
                });
    }

    ProfileDto resetPassword(final SessionContext session, final PasswordResetRequest request)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.USER_MANAGE);
        if (request == null) throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "重置密码请求不能为空");
        IdentityServiceSupport.positive(request.getUserId(), "用户编号");
        IdentityServiceSupport.password(request.getNewPassword(), "新密码");
        final String hash = hasher.hash(request.getNewPassword());
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<ProfileDto>() {
                    @Override public ProfileDto execute(Connection c) throws IdentityServiceException {
                        IdentityUserRecord target = user(c, request.getUserId(), true);
                        if (!repository.resetPassword(c, target.getUserId(), hash)) {
                            throw IdentityServiceSupport.error(ResultCodes.CONFLICT, "密码未更新");
                        }
                        repository.revokeUserSessions(c, target.getUserId());
                        sessionManager.invalidateUserSessions(target.getUserId());
                        IdentityServiceSupport.audit(repository, c, session, "USER_PASSWORD_RESET",
                                "USER", Long.valueOf(target.getUserId()), null);
                        return user(c, target.getUserId(), false).toProfile();
                    }
                });
    }

    private IdentityUserRecord user(Connection c, long id, boolean lock)
            throws IdentityServiceException {
        IdentityServiceSupport.positive(id, "用户编号");
        IdentityUserRecord found = repository.findById(c, id, lock);
        if (found == null) throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "用户不存在");
        return found;
    }

    private static void ensureNotSelf(SessionContext session, long target) throws IdentityServiceException {
        if (session.getUserId() == target) {
            throw IdentityServiceSupport.error(ResultCodes.FORBIDDEN, "不能停用当前登录账号");
        }
    }

    private static void validateStatus(UserStatusUpdateRequest request) throws IdentityServiceException {
        if (request == null) throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "状态请求不能为空");
        IdentityServiceSupport.positive(request.getUserId(), "用户编号");
        String value = IdentityServiceSupport.text(request.getStatus(), "状态").toUpperCase();
        try { UserStatus.valueOf(value); }
        catch (IllegalArgumentException ex) {
            throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "用户状态不正确");
        }
    }
}
