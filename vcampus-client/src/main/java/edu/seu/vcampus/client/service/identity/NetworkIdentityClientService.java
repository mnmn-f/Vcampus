package edu.seu.vcampus.client.service.identity;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;
import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;
import edu.seu.vcampus.common.dto.identity.MonitorSnapshotDto;
import edu.seu.vcampus.common.dto.identity.PasswordChangeRequest;
import edu.seu.vcampus.common.dto.identity.PasswordResetRequest;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.identity.RoleAssignmentRequest;
import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.dto.identity.RoleRevokeRequest;
import edu.seu.vcampus.common.dto.identity.SessionPage;
import edu.seu.vcampus.common.dto.identity.SessionQuery;
import edu.seu.vcampus.common.dto.identity.SessionRevokeRequest;
import edu.seu.vcampus.common.dto.identity.UserPage;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.dto.identity.UserStatusUpdateRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 身份网络客户端；注册可匿名，其余命令只发送当前 ClientSession 的令牌。 */
public final class NetworkIdentityClientService implements IdentityClientService {
    private final NetworkClientService network;
    private final ClientSession session;

    public NetworkIdentityClientService(NetworkClientService network, ClientSession session) {
        if (network == null || session == null) throw new IllegalArgumentException("identity client dependencies required");
        this.network = network;
        this.session = session;
    }

    public NetworkIdentityClientService(ClientGateway gateway, ClientSession session) {
        this(new NetworkClientService(gateway), session);
    }

    @Override public ProfileDto register(RegistrationRequest r) throws NetworkClientException {
        return cast(network.request(IdentityCommands.REGISTER, r), ProfileDto.class);
    }
    @Override public ProfileDto getOwnProfile() throws NetworkClientException {
        return cast(request(IdentityCommands.PROFILE_SELF, null), ProfileDto.class);
    }
    @Override public ProfileDto updateProfile(ProfileUpdateRequest r) throws NetworkClientException {
        return cast(request(IdentityCommands.PROFILE_UPDATE, r), ProfileDto.class);
    }
    @Override public ProfileDto changePassword(PasswordChangeRequest r) throws NetworkClientException {
        return cast(request(IdentityCommands.PASSWORD_CHANGE, r), ProfileDto.class);
    }
    @Override public UserPage searchUsers(UserQuery q) throws NetworkClientException {
        return cast(request(IdentityCommands.USER_SEARCH, q), UserPage.class);
    }
    @Override public ProfileDto updateUserStatus(UserStatusUpdateRequest r) throws NetworkClientException {
        return cast(request(IdentityCommands.USER_STATUS_UPDATE, r), ProfileDto.class);
    }
    @Override public ProfileDto resetPassword(PasswordResetRequest r) throws NetworkClientException {
        return cast(request(IdentityCommands.USER_PASSWORD_RESET, r), ProfileDto.class);
    }
    @Override public List<RoleDto> listRoles() throws NetworkClientException {
        Object value = request(IdentityCommands.ROLE_LIST, null).getPayload();
        if (!(value instanceof List)) throw invalidResponse();
        List<?> raw = (List<?>) value;
        List<RoleDto> result = new ArrayList<RoleDto>();
        for (Object item : raw) {
            if (!(item instanceof RoleDto)) throw invalidResponse();
            result.add((RoleDto) item);
        }
        return result;
    }
    @Override public ProfileDto assignRole(RoleAssignmentRequest r) throws NetworkClientException {
        return cast(request(IdentityCommands.ROLE_ASSIGN, r), ProfileDto.class);
    }
    @Override public ProfileDto revokeRole(RoleRevokeRequest r) throws NetworkClientException {
        return cast(request(IdentityCommands.ROLE_REVOKE, r), ProfileDto.class);
    }
    @Override public SessionPage searchSessions(SessionQuery q) throws NetworkClientException {
        return cast(request(IdentityCommands.SESSION_LIST, q), SessionPage.class);
    }
    @Override public boolean revokeSession(SessionRevokeRequest r) throws NetworkClientException {
        Object value = request(IdentityCommands.SESSION_REVOKE, r).getPayload();
        if (!(value instanceof Boolean)) throw invalidResponse();
        return ((Boolean) value).booleanValue();
    }
    @Override public LoginAuditPage searchLoginAudits(AuditQuery q) throws NetworkClientException {
        return cast(request(IdentityCommands.LOGIN_AUDIT_PAGE, q), LoginAuditPage.class);
    }
    @Override public BusinessAuditPage searchBusinessAudits(AuditQuery q) throws NetworkClientException {
        return cast(request(IdentityCommands.BUSINESS_AUDIT_PAGE, q), BusinessAuditPage.class);
    }
    @Override public MonitorSnapshotDto monitor() throws NetworkClientException {
        return cast(request(IdentityCommands.SYSTEM_MONITOR, null), MonitorSnapshotDto.class);
    }
    @Override public AccountCancellationDto submitAccountCancellation(AccountCancellationRequest r)
            throws NetworkClientException {
        return cast(request(IdentityCommands.ACCOUNT_CANCELLATION_SUBMIT, r), AccountCancellationDto.class);
    }
    @Override public AccountCancellationPage listOwnAccountCancellations(AccountCancellationQuery q)
            throws NetworkClientException {
        return cast(request(IdentityCommands.ACCOUNT_CANCELLATION_SELF_LIST, q), AccountCancellationPage.class);
    }
    @Override public AccountCancellationDto withdrawAccountCancellation(long requestId)
            throws NetworkClientException {
        return cast(request(IdentityCommands.ACCOUNT_CANCELLATION_WITHDRAW,
                Long.valueOf(requestId)), AccountCancellationDto.class);
    }
    @Override public AccountCancellationPage searchAccountCancellationRequests(AccountCancellationQuery q)
            throws NetworkClientException {
        return cast(request(IdentityCommands.ACCOUNT_CANCELLATION_ADMIN_LIST, q), AccountCancellationPage.class);
    }
    @Override public AccountCancellationDto approveAccountCancellation(AccountCancellationReviewRequest r)
            throws NetworkClientException {
        return cast(request(IdentityCommands.ACCOUNT_CANCELLATION_APPROVE, r), AccountCancellationDto.class);
    }
    @Override public AccountCancellationDto rejectAccountCancellation(AccountCancellationReviewRequest r)
            throws NetworkClientException {
        return cast(request(IdentityCommands.ACCOUNT_CANCELLATION_REJECT, r), AccountCancellationDto.class);
    }
    public void synchronizeSessionToken() {
        network.setSessionToken(session.isAuthenticated() ? session.getSessionToken() : null);
    }

    private Message request(String command, Serializable body) throws NetworkClientException {
        if (!session.isAuthenticated()) throw new NetworkClientException(ResultCodes.UNAUTHORIZED, "请先登录");
        network.setSessionToken(session.getSessionToken());
        return network.request(command, body);
    }

    private static <T> T cast(Message response, Class<T> type) throws NetworkClientException {
        Object value = response == null ? null : response.getPayload();
        if (!type.isInstance(value)) throw invalidResponse();
        return type.cast(value);
    }

    private static NetworkClientException invalidResponse() {
        return new NetworkClientException(ResultCodes.INTERNAL_ERROR, "身份响应格式不正确");
    }
}
