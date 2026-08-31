package edu.seu.vcampus.server.identity.handler;

import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;
import edu.seu.vcampus.common.dto.identity.PasswordChangeRequest;
import edu.seu.vcampus.common.dto.identity.PasswordResetRequest;
import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.identity.RoleAssignmentRequest;
import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.dto.identity.RoleRevokeRequest;
import edu.seu.vcampus.common.dto.identity.SessionQuery;
import edu.seu.vcampus.common.dto.identity.SessionRevokeRequest;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.dto.identity.UserStatusUpdateRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.identity.service.IdentityService;
import edu.seu.vcampus.server.identity.service.IdentityServiceException;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

import java.util.ArrayList;

/** 身份命令适配层；REGISTER 是唯一明确无需认证的命令。 */
public final class IdentityCommandHandler implements CommandHandler {
    private final String command;
    private final IdentityService service;

    public IdentityCommandHandler(String command, IdentityService service) {
        if (command == null || command.trim().isEmpty() || service == null) {
            throw new IllegalArgumentException("identity handler dependencies are required");
        }
        this.command = command;
        this.service = service;
    }

    @Override public Message handle(Message request, SessionContext session) {
        try {
            Object p = request == null ? null : request.getPayload();
            if (is(IdentityCommands.REGISTER)) return Message.success(request,
                    service.register(required(p, RegistrationRequest.class)));
            if (is(IdentityCommands.PROFILE_SELF)) return Message.success(request,
                    service.getOwnProfile(session));
            if (is(IdentityCommands.PROFILE_UPDATE)) return Message.success(request,
                    service.updateProfile(session, required(p, ProfileUpdateRequest.class)));
            if (is(IdentityCommands.PASSWORD_CHANGE)) return Message.success(request,
                    service.changePassword(session, required(p, PasswordChangeRequest.class)));
            if (is(IdentityCommands.USER_SEARCH)) return Message.success(request,
                    service.searchUsers(session, optional(p, UserQuery.class, new UserQuery())));
            if (is(IdentityCommands.USER_STATUS_UPDATE)) return Message.success(request,
                    service.updateUserStatus(session, required(p, UserStatusUpdateRequest.class)));
            if (is(IdentityCommands.USER_PASSWORD_RESET)) return Message.success(request,
                    service.resetPassword(session, required(p, PasswordResetRequest.class)));
            if (is(IdentityCommands.ROLE_LIST)) return Message.success(request,
                    new ArrayList<RoleDto>(service.listRoles(session)));
            if (is(IdentityCommands.ROLE_ASSIGN)) return Message.success(request,
                    service.assignRole(session, required(p, RoleAssignmentRequest.class)));
            if (is(IdentityCommands.ROLE_REVOKE)) return Message.success(request,
                    service.revokeRole(session, required(p, RoleRevokeRequest.class)));
            if (is(IdentityCommands.SESSION_LIST)) return Message.success(request,
                    service.searchSessions(session, optional(p, SessionQuery.class, new SessionQuery())));
            if (is(IdentityCommands.SESSION_REVOKE)) return Message.success(request,
                    Boolean.valueOf(service.revokeSession(session,
                            required(p, SessionRevokeRequest.class))));
            if (is(IdentityCommands.LOGIN_AUDIT_PAGE)) return Message.success(request,
                    service.searchLoginAudits(session, optional(p, AuditQuery.class, new AuditQuery())));
            if (is(IdentityCommands.BUSINESS_AUDIT_PAGE)) return Message.success(request,
                    service.searchBusinessAudits(session, optional(p, AuditQuery.class, new AuditQuery())));
            if (is(IdentityCommands.SYSTEM_MONITOR)) return Message.success(request, service.monitor(session));
            if (is(IdentityCommands.ACCOUNT_CANCELLATION_SUBMIT)) return Message.success(request,
                    service.submitAccountCancellation(session, required(p, AccountCancellationRequest.class)));
            if (is(IdentityCommands.ACCOUNT_CANCELLATION_SELF_LIST)) return Message.success(request,
                    service.listOwnAccountCancellations(session,
                            optional(p, AccountCancellationQuery.class, new AccountCancellationQuery())));
            if (is(IdentityCommands.ACCOUNT_CANCELLATION_WITHDRAW)) return Message.success(request,
                    service.withdrawAccountCancellation(session, requestId(p)));
            if (is(IdentityCommands.ACCOUNT_CANCELLATION_ADMIN_LIST)) return Message.success(request,
                    service.searchAccountCancellationRequests(session,
                            optional(p, AccountCancellationQuery.class, new AccountCancellationQuery())));
            if (is(IdentityCommands.ACCOUNT_CANCELLATION_APPROVE)) return Message.success(request,
                    service.approveAccountCancellation(session,
                            required(p, AccountCancellationReviewRequest.class)));
            if (is(IdentityCommands.ACCOUNT_CANCELLATION_REJECT)) return Message.success(request,
                    service.rejectAccountCancellation(session,
                            required(p, AccountCancellationReviewRequest.class)));
            return Message.failure(request, ResultCodes.INVALID_INPUT, "不支持的身份操作");
        } catch (IdentityServiceException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        } catch (IllegalArgumentException ex) {
            return Message.failure(request, ResultCodes.INVALID_INPUT, "请求参数格式不正确");
        } catch (RuntimeException ex) {
            return Message.failure(request, ResultCodes.INTERNAL_ERROR, "身份服务暂时不可用");
        }
    }

    @Override public Permission requiredPermission() {
        if (is(IdentityCommands.REGISTER)) return null;
        if (is(IdentityCommands.PROFILE_SELF)) return Permission.PROFILE_READ;
        if (is(IdentityCommands.PROFILE_UPDATE) || is(IdentityCommands.PASSWORD_CHANGE)) {
            return Permission.PROFILE_UPDATE;
        }
        if (is(IdentityCommands.USER_SEARCH) || is(IdentityCommands.USER_STATUS_UPDATE)
                || is(IdentityCommands.USER_PASSWORD_RESET) || is(IdentityCommands.SESSION_LIST)
                || is(IdentityCommands.SESSION_REVOKE)) return Permission.USER_MANAGE;
        if (is(IdentityCommands.ROLE_LIST) || is(IdentityCommands.ROLE_ASSIGN)
                || is(IdentityCommands.ROLE_REVOKE)) return Permission.ROLE_MANAGE;
        if (is(IdentityCommands.ACCOUNT_CANCELLATION_SUBMIT)
                || is(IdentityCommands.ACCOUNT_CANCELLATION_WITHDRAW)) return Permission.PROFILE_UPDATE;
        if (is(IdentityCommands.ACCOUNT_CANCELLATION_SELF_LIST)) return Permission.PROFILE_READ;
        if (is(IdentityCommands.ACCOUNT_CANCELLATION_ADMIN_LIST)
                || is(IdentityCommands.ACCOUNT_CANCELLATION_APPROVE)
                || is(IdentityCommands.ACCOUNT_CANCELLATION_REJECT)) return Permission.USER_MANAGE;
        return Permission.SYSTEM_MONITOR;
    }

    @Override public boolean requiresAuthentication() { return !is(IdentityCommands.REGISTER); }

    private boolean is(String value) { return value.equals(command); }

    private static <T> T required(Object value, Class<T> type) {
        if (!type.isInstance(value)) throw new IllegalArgumentException("invalid identity payload");
        return type.cast(value);
    }

    private static <T> T optional(Object value, Class<T> type, T fallback) {
        return value == null ? fallback : required(value, type);
    }

    private static long requestId(Object value) {
        if (!(value instanceof Number)) throw new IllegalArgumentException("invalid cancellation request id");
        return ((Number) value).longValue();
    }
}
