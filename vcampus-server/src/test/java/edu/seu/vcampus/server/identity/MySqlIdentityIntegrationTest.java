package edu.seu.vcampus.server.identity;

import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;
import edu.seu.vcampus.common.dto.identity.PasswordChangeRequest;
import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.identity.RoleAssignmentRequest;
import edu.seu.vcampus.common.dto.identity.RoleRevokeRequest;
import edu.seu.vcampus.common.dto.identity.UserStatusUpdateRequest;
import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.auth.AuthService;
import edu.seu.vcampus.server.auth.AuthenticationException;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.registry.IdentityCommandRegistry;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlIdentityUserRepository;
import edu.seu.vcampus.server.identity.repository.mysql.MySqlLoginAuditSink;
import edu.seu.vcampus.server.identity.service.IdentityService;
import edu.seu.vcampus.server.repository.MySqlUserRepository;
import edu.seu.vcampus.server.repository.UserRecord;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 可选真实 MySQL 身份链：用户、角色、状态、密码、认证和两类审计。 */
public final class MySqlIdentityIntegrationTest {
    private static final String INITIAL_PASSWORD = "IdentityDb123";
    private static final String CHANGED_PASSWORD = "IdentityDb456";
    private JdbcConnectionFactory factory;
    private TransactionManager transactions;
    private IdentityService identity;
    private MySqlUserRepository rootUsers;
    private AuthService auth;
    private PasswordHasher hasher;
    private SessionManager sessions;
    private SessionContext admin;
    private long testUserId;
    private String account;

    @Before public void setUp() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        factory = new JdbcConnectionFactory(System.getProperty("vcampus.db.url",
                JdbcConnectionFactory.DEFAULT_URL), System.getProperty("vcampus.db.user",
                JdbcConnectionFactory.DEFAULT_USER), System.getProperty("vcampus.db.password",
                JdbcConnectionFactory.DEFAULT_PASSWORD));
        transactions = new TransactionManager(factory);
        sessions = new SessionManager();
        hasher = new PasswordHasher(4);
        identity = IdentityCommandRegistry.createMySqlService(transactions, hasher, sessions);
        rootUsers = new MySqlUserRepository(factory);
        auth = new AuthService(rootUsers, hasher, sessions, new MySqlLoginAuditSink(factory));
        LoginResult adminLogin = auth.login("demo_system", "system123");
        admin = sessions.resolve(adminLogin.getSessionToken());
        account = "dbit_identity_" + Long.toString(System.nanoTime());
    }

    @After public void cleanUp() throws Exception {
        if (sessions != null) sessions.invalidateUserSessions(testUserId);
        if (transactions != null && testUserId > 0L) {
            final long id = testUserId;
            transactions.execute(new TransactionWork<Void>() {
                @Override public Void execute(Connection c) throws Exception {
                    delete(c, "DELETE FROM user_roles WHERE user_id=?", id);
                    delete(c, "DELETE FROM users WHERE id=?", id);
                    return null;
                }
            });
        }
    }

    @Test public void identityWriteReadAuthAndAuditRoundTrip() throws Exception {
        ProfileDto registered = identity.register(new RegistrationRequest(account,
                INITIAL_PASSWORD, "身份集成探针"));
        testUserId = registered.getUserId();
        assertEquals(account, registered.getAccount());
        assertTrue(registered.getRoles().contains(Role.STUDENT));

        LoginResult firstLogin = auth.login(new LoginRequest(account, INITIAL_PASSWORD));
        SessionContext student = sessions.resolve(firstLogin.getSessionToken());
        ProfileDto self = identity.getOwnProfile(student);
        assertEquals(testUserId, self.getUserId());
        assertEquals("ACTIVE", self.getStatus());
        ProfileDto updated = identity.updateProfile(student,
                new ProfileUpdateRequest("身份集成探针更新", null, null, null));
        assertEquals("身份集成探针更新", updated.getDisplayName());
        identity.changePassword(student, new PasswordChangeRequest(INITIAL_PASSWORD,
                CHANGED_PASSWORD));
        assertNotNull(auth.login(account, CHANGED_PASSWORD));

        ProfileDto assigned = identity.assignRole(admin,
                new RoleAssignmentRequest(testUserId, Role.TEACHER));
        assertTrue(assigned.getRoles().contains(Role.TEACHER));
        ProfileDto revoked = identity.revokeRole(admin,
                new RoleRevokeRequest(testUserId, Role.TEACHER));
        assertFalse(revoked.getRoles().contains(Role.TEACHER));

        assertEquals("DISABLED", identity.updateUserStatus(admin,
                new UserStatusUpdateRequest(testUserId, "DISABLED")).getStatus());
        try {
            auth.login(account, CHANGED_PASSWORD);
            fail("disabled account must not authenticate");
        } catch (AuthenticationException expected) {
            assertEquals(ResultCodes.ACCOUNT_DISABLED, expected.getResultCode());
        }
        assertEquals("ACTIVE", identity.updateUserStatus(admin,
                new UserStatusUpdateRequest(testUserId, "ACTIVE")).getStatus());

        UserRecord rootRecord = rootUsers.findByAccount(account);
        rootUsers.save(rootRecord);
        assertTrue(rootUsers.findByAccount(account).getRoles().contains(Role.STUDENT));
        assertNotNull(auth.login(account, CHANGED_PASSWORD));

        LoginAuditPage logins = identity.searchLoginAudits(admin,
                new AuditQuery(Long.valueOf(testUserId), null, null, 1, 100));
        assertTrue(logins.getTotal() >= 3L);
        BusinessAuditPage business = identity.searchBusinessAudits(admin,
                new AuditQuery(Long.valueOf(admin.getUserId()), "ROLE_ASSIGN", "SUCCESS", 1, 20));
        assertTrue(business.getTotal() >= 1L);
    }

    @Test public void loginMatchesStudentAndEmployeeIdentifiers() throws Exception {
        LoginResult student = auth.login("DEMO2026001", "student123");
        LoginResult teacher = auth.login("DEMO-T-001", "teacher123");
        assertEquals("demo_student", student.getAccount());
        assertEquals("演示学生", student.getDisplayName());
        assertEquals("demo_teacher", teacher.getAccount());
        assertEquals("演示教师兼教务员", teacher.getDisplayName());
        assertTrue(sessions.resolve(student.getSessionToken()) != null);
        assertTrue(sessions.resolve(teacher.getSessionToken()) != null);
        auth.logout(student.getSessionToken());
        auth.logout(teacher.getSessionToken());
    }

    @Test public void identityInsertRollsBackWithoutLeavingAnAccount() throws Exception {
        final String rollbackAccount = "dbit_identity_rollback_" + Long.toString(System.nanoTime());
        try {
            transactions.execute(new TransactionWork<Void>() {
                @Override public Void execute(Connection c) throws Exception {
                    new MySqlIdentityUserRepository().insertStudent(c,
                            new RegistrationRequest(rollbackAccount, INITIAL_PASSWORD, "回滚探针"),
                            hasher.hash(INITIAL_PASSWORD));
                    throw new SQLException("identity rollback probe");
                }
            });
            fail("identity insert rollback probe must fail");
        } catch (SQLException expected) {
            assertEquals("identity rollback probe", expected.getMessage());
        }
        assertTrue(rootUsers.findByAccount(rollbackAccount) == null);
    }

    private static void delete(Connection c, String sql, long id) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
