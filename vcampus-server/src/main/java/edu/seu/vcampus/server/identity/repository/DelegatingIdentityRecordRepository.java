package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditWriteRequest;
import edu.seu.vcampus.common.dto.identity.MonitorSnapshotDto;
import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.dto.identity.SessionPage;
import edu.seu.vcampus.common.dto.identity.SessionQuery;
import edu.seu.vcampus.common.dto.identity.UserPage;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;
import java.util.List;

/** 将分职责身份仓储组合为统一事务入口。 */
public class DelegatingIdentityRecordRepository implements IdentityRecordRepository {
    private final IdentityUserRepository users;
    private final IdentityRoleRepository roles;
    private final IdentitySessionRepository sessions;
    private final IdentityAuditRepository audits;
    private final IdentityMonitorRepository monitor;
    private final IdentityCancellationRepository cancellations;

    public DelegatingIdentityRecordRepository(IdentityUserRepository users,
                                              IdentityRoleRepository roles,
                                              IdentitySessionRepository sessions,
                                              IdentityAuditRepository audits,
                                              IdentityMonitorRepository monitor) {
        this(users, roles, sessions, audits, monitor, null);
    }

    public DelegatingIdentityRecordRepository(IdentityUserRepository users,
                                              IdentityRoleRepository roles,
                                              IdentitySessionRepository sessions,
                                              IdentityAuditRepository audits,
                                              IdentityMonitorRepository monitor,
                                              IdentityCancellationRepository cancellations) {
        if (users == null || roles == null || sessions == null || audits == null || monitor == null) {
            throw new IllegalArgumentException("identity repositories are required");
        }
        this.users = users;
        this.roles = roles;
        this.sessions = sessions;
        this.audits = audits;
        this.monitor = monitor;
        this.cancellations = cancellations;
    }

    @Override public IdentityUserRecord findById(Connection c, long id, boolean lock) {
        return users.findById(c, id, lock);
    }
    @Override public IdentityUserRecord findByAccount(Connection c, String a, boolean lock) {
        return users.findByAccount(c, a, lock);
    }
    @Override public UserPage search(Connection c, UserQuery q) { return users.search(c, q); }
    @Override public long insertStudent(Connection c, RegistrationRequest r, String h) {
        return users.insertStudent(c, r, h);
    }
    @Override public boolean updateProfile(Connection c, long id, ProfileUpdateRequest r) {
        return users.updateProfile(c, id, r);
    }
    @Override public boolean updatePassword(Connection c, long id, String oldHash, String newHash) {
        return users.updatePassword(c, id, oldHash, newHash);
    }
    @Override public boolean resetPassword(Connection c, long id, String hash) {
        return users.resetPassword(c, id, hash);
    }
    @Override public boolean updateStatus(Connection c, long id, String status) {
        return users.updateStatus(c, id, status);
    }
    @Override public boolean assignRole(Connection c, long id, Role role, long operator) {
        return users.assignRole(c, id, role, operator);
    }
    @Override public boolean revokeRole(Connection c, long id, Role role) {
        return users.revokeRole(c, id, role);
    }
    @Override public int countRoles(Connection c, long id) { return users.countRoles(c, id); }

    @Override public List<RoleDto> listRoles(Connection c) { return roles.listRoles(c); }
    @Override public IdentityRoleRecord findRole(Connection c, Role role, boolean lock) {
        return roles.findRole(c, role, lock);
    }

    @Override public SessionPage search(Connection c, SessionQuery q) { return sessions.search(c, q); }
    @Override public boolean revoke(Connection c, long id) { return sessions.revoke(c, id); }
    @Override public int revokeUserSessions(Connection c, long id) { return sessions.revokeUserSessions(c, id); }
    @Override public int revokePersistedUserSessions(Connection c, long id) {
        return sessions.revokePersistedUserSessions(c, id);
    }
    @Override public void observe(SessionContext session) { sessions.observe(session); }

    @Override public void insertLoginAudit(Connection c, LoginAuditWriteRequest r) {
        audits.insertLoginAudit(c, r);
    }
    @Override public void insertBusinessAudit(Connection c, SessionContext a, String action,
                                              String type, Long id, String outcome, String detail) {
        audits.insertBusinessAudit(c, a, action, type, id, outcome, detail);
    }
    @Override public LoginAuditPage searchLoginAudits(Connection c, AuditQuery q) {
        return audits.searchLoginAudits(c, q);
    }
    @Override public BusinessAuditPage searchBusinessAudits(Connection c, AuditQuery q) {
        return audits.searchBusinessAudits(c, q);
    }

    @Override public MonitorSnapshotDto snapshot(Connection c) { return monitor.snapshot(c); }

    @Override public AccountCancellationPage searchAccountCancellations(Connection c,
                                                                          AccountCancellationQuery q,
                                                                          Long userId) {
        return cancellationRepository().searchAccountCancellations(c, q, userId);
    }

    @Override public AccountCancellationRecord findAccountCancellation(Connection c,
                                                                                    long id,
                                                                                    boolean lock) {
        return cancellationRepository().findAccountCancellation(c, id, lock);
    }

    @Override public AccountCancellationRecord findPendingAccountCancellation(Connection c,
                                                                                         long userId,
                                                                                         boolean lock) {
        return cancellationRepository().findPendingAccountCancellation(c, userId, lock);
    }

    @Override public long insertAccountCancellation(Connection c, long userId, String reason) {
        return cancellationRepository().insertAccountCancellation(c, userId, reason);
    }

    @Override public boolean withdrawAccountCancellation(Connection c, long requestId, long userId) {
        return cancellationRepository().withdrawAccountCancellation(c, requestId, userId);
    }

    @Override public boolean reviewAccountCancellation(Connection c, long requestId, String status,
                                                       long reviewerId, String remark) {
        return cancellationRepository().reviewAccountCancellation(c, requestId, status,
                reviewerId, remark);
    }

    private IdentityCancellationRepository cancellationRepository() {
        if (cancellations == null) {
            throw new IdentityRepositoryException("账号注销申请仓储未配置");
        }
        return cancellations;
    }
}
