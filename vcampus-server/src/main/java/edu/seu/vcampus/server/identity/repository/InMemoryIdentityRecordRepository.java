package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.BusinessAuditDto;
import edu.seu.vcampus.common.dto.identity.LoginAuditDto;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.repository.InMemoryUserRepository;
import edu.seu.vcampus.server.repository.UserRepository;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import org.threeten.bp.LocalDateTime;
import java.util.EnumSet;

/** 身份内存仓储总入口；可与现有 AuthService 的 UserRepository 共享用户。 */
public class InMemoryIdentityRecordRepository extends DelegatingIdentityRecordRepository {
    private final InMemoryIdentityState state;
    private final InMemoryIdentityUserRepository users;
    private final InMemoryIdentitySessionRepository sessions;
    private final InMemoryIdentityAuditRepository audits;
    private final SessionManager sessionManager;

    public InMemoryIdentityRecordRepository() {
        this(new InMemoryUserRepository(), new SessionManager());
    }

    public InMemoryIdentityRecordRepository(UserRepository authUsers, SessionManager sessionManager) {
        this(new InMemoryIdentityState(), authUsers == null ? new InMemoryUserRepository() : authUsers,
                sessionManager == null ? new SessionManager() : sessionManager);
    }

    private InMemoryIdentityRecordRepository(InMemoryIdentityState state, UserRepository authUsers,
                                             SessionManager sessionManager) {
        this(state, new InMemoryIdentityUserRepository(state, authUsers),
                new InMemoryIdentityRoleRepository(state),
                new InMemoryIdentitySessionRepository(state, sessionManager),
                new InMemoryIdentityAuditRepository(state),
                new InMemoryIdentityMonitorRepository(state),
                new InMemoryIdentityCancellationRepository(state), sessionManager);
    }

    private InMemoryIdentityRecordRepository(InMemoryIdentityState state,
                                             InMemoryIdentityUserRepository users,
                                             IdentityRoleRepository roles,
                                             InMemoryIdentitySessionRepository sessions,
                                             InMemoryIdentityAuditRepository audits,
                                             IdentityMonitorRepository monitor,
                                             IdentityCancellationRepository cancellations,
                                             SessionManager sessionManager) {
        super(users, roles, sessions, audits, monitor, cancellations);
        this.state = state;
        this.users = users;
        this.sessions = sessions;
        this.audits = audits;
        this.sessionManager = sessionManager;
    }

    public void addUser(IdentityUserRecord user) { users.add(user); }

    public void addUser(long userId, String account, String passwordHash, String displayName,
                        Role role) {
        addUser(new IdentityUserRecord(userId, account, passwordHash, displayName, null, null,
                null, "ACTIVE", EnumSet.of(role), LocalDateTime.now(), LocalDateTime.now(), null));
    }

    public void addSession(SessionContext session) { sessions.add(session); }

    public void addLoginAudit(LoginAuditDto value) {
        audits.addLogin(value);
    }

    public void addBusinessAudit(BusinessAuditDto value) {
        audits.addBusiness(value);
    }

    public UserRepository getAuthRepository() { return users.authRepository(); }
    public SessionManager getSessionManager() { return sessionManager; }
    public IdentityUserRecord getUser(long userId) { return findById(null, userId, false); }
    public Object transactionLock() { return state; }
    public Object snapshotState() { synchronized (state) { return state.snapshot(); } }

    public void restoreState(Object snapshot) {
        if (!(snapshot instanceof InMemoryIdentityState.Snapshot)) {
            throw new IllegalArgumentException("invalid identity snapshot");
        }
        synchronized (state) { state.restore((InMemoryIdentityState.Snapshot) snapshot); }
    }

}
