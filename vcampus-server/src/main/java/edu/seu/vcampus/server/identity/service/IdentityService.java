package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditWriteRequest;
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
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.InMemoryIdentityRecordRepository;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.util.List;

/** 身份资料与系统管理的统一服务门面。 */
public final class IdentityService {
    private final IdentityProfileService profiles;
    private final IdentityUserAdminService users;
    private final IdentityRoleService roles;
    private final IdentitySessionService sessions;
    private final IdentityAuditService audits;
    private final IdentityCancellationService cancellations;
    private final SessionManager sessionManager;

    public IdentityService(IdentityRecordRepository repository, PasswordHasher hasher,
                           SessionManager sessionManager, IdentityTransactionRunner transactions) {
        if (repository == null || hasher == null || transactions == null) {
            throw new IllegalArgumentException("identity service dependencies are required");
        }
        SessionManager runtime = sessionManager;
        if (runtime == null && repository instanceof InMemoryIdentityRecordRepository) {
            runtime = ((InMemoryIdentityRecordRepository) repository).getSessionManager();
        }
        if (runtime == null) runtime = new SessionManager();
        this.sessionManager = runtime;
        profiles = new IdentityProfileService(repository, transactions, hasher);
        users = new IdentityUserAdminService(repository, transactions, hasher, runtime);
        roles = new IdentityRoleService(repository, transactions, runtime);
        sessions = new IdentitySessionService(repository, transactions, runtime);
        audits = new IdentityAuditService(repository, transactions, runtime);
        cancellations = new IdentityCancellationService(repository, transactions, runtime);
    }

    public IdentityService(IdentityRecordRepository repository, PasswordHasher hasher,
                           SessionManager sessionManager, TransactionManager transactions) {
        this(repository, hasher, sessionManager, new IdentityTransactionManagerRunner(transactions));
    }

    public IdentityService(IdentityRecordRepository repository, PasswordHasher hasher,
                           TransactionManager transactions) {
        this(repository, hasher, new SessionManager(), transactions);
    }

    public IdentityService(IdentityRecordRepository repository, PasswordHasher hasher,
                           IdentityTransactionRunner transactions) {
        this(repository, hasher, null, transactions);
    }

    public ProfileDto register(RegistrationRequest request) throws IdentityServiceException { return profiles.register(request); }
    public ProfileDto getOwnProfile(SessionContext session) throws IdentityServiceException { return profiles.own(session); }
    public ProfileDto getProfile(SessionContext session) throws IdentityServiceException { return profiles.own(session); }
    public ProfileDto updateOwnProfile(SessionContext s, ProfileUpdateRequest r) throws IdentityServiceException { return profiles.update(s, r); }
    public ProfileDto updateProfile(SessionContext s, ProfileUpdateRequest r) throws IdentityServiceException { return profiles.update(s, r); }
    public ProfileDto changePassword(SessionContext s, PasswordChangeRequest r) throws IdentityServiceException { return profiles.changePassword(s, r); }
    public ProfileDto updateUserStatus(SessionContext s, UserStatusUpdateRequest r) throws IdentityServiceException { return users.updateStatus(s, r); }
    public ProfileDto setUserStatus(SessionContext s, UserStatusUpdateRequest r) throws IdentityServiceException { return users.updateStatus(s, r); }
    public ProfileDto resetPassword(SessionContext s, PasswordResetRequest r) throws IdentityServiceException { return users.resetPassword(s, r); }
    public ProfileDto resetUserPassword(SessionContext s, PasswordResetRequest r) throws IdentityServiceException { return users.resetPassword(s, r); }
    public UserPage searchUsers(SessionContext s, UserQuery q) throws IdentityServiceException { return users.search(s, q); }
    public UserPage userPage(SessionContext s, UserQuery q) throws IdentityServiceException { return users.search(s, q); }
    public List<RoleDto> listRoles(SessionContext s) throws IdentityServiceException { return roles.list(s); }
    public ProfileDto assignRole(SessionContext s, RoleAssignmentRequest r) throws IdentityServiceException { return roles.assign(s, r); }
    public ProfileDto revokeRole(SessionContext s, RoleRevokeRequest r) throws IdentityServiceException { return roles.revoke(s, r); }
    public SessionPage searchSessions(SessionContext s, SessionQuery q) throws IdentityServiceException { return sessions.search(s, q); }
    public SessionPage listSessions(SessionContext s, SessionQuery q) throws IdentityServiceException { return sessions.search(s, q); }
    public boolean revokeSession(SessionContext s, SessionRevokeRequest r) throws IdentityServiceException { return sessions.revoke(s, r); }
    public LoginAuditPage searchLoginAudits(SessionContext s, AuditQuery q) throws IdentityServiceException { return audits.loginPage(s, q); }
    public BusinessAuditPage searchBusinessAudits(SessionContext s, AuditQuery q) throws IdentityServiceException { return audits.businessPage(s, q); }
    public MonitorSnapshotDto monitor(SessionContext s) throws IdentityServiceException { return audits.monitor(s); }
    public AccountCancellationDto submitAccountCancellation(SessionContext s,
                                                             AccountCancellationRequest r)
            throws IdentityServiceException { return cancellations.submit(s, r); }
    public AccountCancellationPage listOwnAccountCancellations(SessionContext s,
                                                                AccountCancellationQuery q)
            throws IdentityServiceException { return cancellations.own(s, q); }
    public AccountCancellationDto withdrawAccountCancellation(SessionContext s, long requestId)
            throws IdentityServiceException { return cancellations.withdraw(s, requestId); }
    public AccountCancellationPage searchAccountCancellationRequests(SessionContext s,
                                                                      AccountCancellationQuery q)
            throws IdentityServiceException { return cancellations.search(s, q); }
    public AccountCancellationDto approveAccountCancellation(SessionContext s,
                                                             AccountCancellationReviewRequest r)
            throws IdentityServiceException { return cancellations.approve(s, r); }
    public AccountCancellationDto rejectAccountCancellation(SessionContext s,
                                                            AccountCancellationReviewRequest r)
            throws IdentityServiceException { return cancellations.reject(s, r); }
    public void recordLoginAudit(LoginAuditWriteRequest r) throws IdentityServiceException { audits.writeLogin(r); }
    public void observeSession(SessionContext s) { sessions.observe(s); }
    public SessionManager getSessionManager() { return sessionManager; }
}
