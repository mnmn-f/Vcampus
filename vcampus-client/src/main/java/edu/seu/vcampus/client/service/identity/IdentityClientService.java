package edu.seu.vcampus.client.service.identity;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;
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

import java.util.List;

/** 身份资料与系统管理的网络服务接口，不依赖演示 UI。 */
public interface IdentityClientService {
    ProfileDto register(RegistrationRequest request) throws NetworkClientException;
    ProfileDto getOwnProfile() throws NetworkClientException;
    ProfileDto updateProfile(ProfileUpdateRequest request) throws NetworkClientException;
    ProfileDto changePassword(PasswordChangeRequest request) throws NetworkClientException;
    UserPage searchUsers(UserQuery query) throws NetworkClientException;
    ProfileDto updateUserStatus(UserStatusUpdateRequest request) throws NetworkClientException;
    ProfileDto resetPassword(PasswordResetRequest request) throws NetworkClientException;
    List<RoleDto> listRoles() throws NetworkClientException;
    ProfileDto assignRole(RoleAssignmentRequest request) throws NetworkClientException;
    ProfileDto revokeRole(RoleRevokeRequest request) throws NetworkClientException;
    SessionPage searchSessions(SessionQuery query) throws NetworkClientException;
    boolean revokeSession(SessionRevokeRequest request) throws NetworkClientException;
    LoginAuditPage searchLoginAudits(AuditQuery query) throws NetworkClientException;
    BusinessAuditPage searchBusinessAudits(AuditQuery query) throws NetworkClientException;
    MonitorSnapshotDto monitor() throws NetworkClientException;
    AccountCancellationDto submitAccountCancellation(AccountCancellationRequest request)
            throws NetworkClientException;
    AccountCancellationPage listOwnAccountCancellations(AccountCancellationQuery query)
            throws NetworkClientException;
    AccountCancellationDto withdrawAccountCancellation(long requestId)
            throws NetworkClientException;
    AccountCancellationPage searchAccountCancellationRequests(AccountCancellationQuery query)
            throws NetworkClientException;
    AccountCancellationDto approveAccountCancellation(AccountCancellationReviewRequest request)
            throws NetworkClientException;
    AccountCancellationDto rejectAccountCancellation(AccountCancellationReviewRequest request)
            throws NetworkClientException;
}
