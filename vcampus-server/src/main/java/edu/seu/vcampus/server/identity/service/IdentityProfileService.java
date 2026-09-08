package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.dto.identity.PasswordChangeRequest;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.IdentityUserRecord;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;

/** 公开注册与当前用户资料/密码用例。 */
final class IdentityProfileService {
    private final IdentityRecordRepository repository;
    private final IdentityTransactionRunner transactions;
    private final PasswordHasher hasher;

    IdentityProfileService(IdentityRecordRepository repository,
                           IdentityTransactionRunner transactions, PasswordHasher hasher) {
        this.repository = repository;
        this.transactions = transactions;
        this.hasher = hasher;
    }

    ProfileDto register(final RegistrationRequest request) throws IdentityServiceException {
        validateRegistration(request);
        final String hash = hasher.hash(request.getPassword());
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<ProfileDto>() {
                    @Override public ProfileDto execute(Connection c) throws IdentityServiceException {
                        if (repository.findByAccount(c, request.getAccount().trim(), true) != null) {
                            throw IdentityServiceSupport.error(ResultCodes.CONFLICT, "账号已存在");
                        }
                        long id = repository.insertStudent(c, request, hash);
                        IdentityUserRecord user = user(c, id, false);
                        IdentityServiceSupport.audit(repository, c, null, "IDENTITY_REGISTER",
                                "USER", Long.valueOf(id), "{\"defaultRole\":\"STUDENT\"}");
                        return user.toProfile();
                    }
                });
    }

    ProfileDto own(final SessionContext session) throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.PROFILE_READ);
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<ProfileDto>() {
                    @Override public ProfileDto execute(Connection c) throws IdentityServiceException {
                        return user(c, session.getUserId(), false).toProfile();
                    }
                });
    }

    ProfileDto update(final SessionContext session, final ProfileUpdateRequest request)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.PROFILE_UPDATE);
        validateProfile(request);
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<ProfileDto>() {
                    @Override public ProfileDto execute(Connection c) throws IdentityServiceException {
                        if (!repository.updateProfile(c, session.getUserId(), request)) {
                            throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "用户不存在");
                        }
                        IdentityServiceSupport.audit(repository, c, session, "PROFILE_UPDATE",
                                "USER", Long.valueOf(session.getUserId()), null);
                        return user(c, session.getUserId(), false).toProfile();
                    }
                });
    }

    ProfileDto changePassword(final SessionContext session, final PasswordChangeRequest request)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.PROFILE_UPDATE);
        if (request == null) throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "密码请求不能为空");
        IdentityServiceSupport.password(request.getCurrentPassword(), "当前密码");
        IdentityServiceSupport.password(request.getNewPassword(), "新密码");
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "新密码不能与旧密码相同");
        }
        final String newHash = hasher.hash(request.getNewPassword());
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<ProfileDto>() {
                    @Override public ProfileDto execute(Connection c) throws IdentityServiceException {
                        IdentityUserRecord old = user(c, session.getUserId(), true);
                        if (!hasher.matches(request.getCurrentPassword(), old.getPasswordHash())) {
                            throw IdentityServiceSupport.error(ResultCodes.FORBIDDEN, "当前密码不正确");
                        }
                        if (!repository.updatePassword(c, old.getUserId(), old.getPasswordHash(), newHash)) {
                            throw IdentityServiceSupport.error(ResultCodes.CONFLICT, "密码已被其他操作修改");
                        }
                        IdentityServiceSupport.audit(repository, c, session, "PASSWORD_CHANGE",
                                "USER", Long.valueOf(old.getUserId()), null);
                        return user(c, old.getUserId(), false).toProfile();
                    }
                });
    }

    private IdentityUserRecord user(Connection c, long id, boolean lock)
            throws IdentityServiceException {
        IdentityUserRecord found = repository.findById(c, id, lock);
        if (found == null) throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "用户不存在");
        return found;
    }

    private static void validateRegistration(RegistrationRequest request)
            throws IdentityServiceException {
        if (request == null) throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "注册请求不能为空");
        String account = IdentityServiceSupport.text(request.getAccount(), "账号");
        IdentityServiceSupport.length(account, 64, "账号");
        if (!account.matches("[A-Za-z0-9_.@-]{3,64}")) {
            throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "账号格式不正确");
        }
        IdentityServiceSupport.password(request.getPassword(), "密码");
        IdentityServiceSupport.text(request.getDisplayName(), "姓名");
        IdentityServiceSupport.length(request.getDisplayName(), 100, "姓名");
        IdentityServiceSupport.length(request.getEmail(), 255, "邮箱");
        validatePhone(request.getPhone());
    }

    private static void validateProfile(ProfileUpdateRequest request)
            throws IdentityServiceException {
        if (request == null) throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "资料请求不能为空");
        IdentityServiceSupport.text(request.getDisplayName(), "姓名");
        IdentityServiceSupport.length(request.getDisplayName(), 100, "姓名");
        IdentityServiceSupport.length(request.getEmail(), 255, "邮箱");
        validatePhone(request.getPhone());
        IdentityServiceSupport.length(request.getAvatarUrl(), 500, "头像地址");
    }

    private static void validatePhone(String phone) throws IdentityServiceException {
        IdentityServiceSupport.length(phone, 32, "手机号");
        if (phone != null && !phone.trim().isEmpty()
                && !phone.trim().matches("[0-9]{6,32}")) {
            throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "手机号只能填写数字");
        }
    }
}
