package edu.seu.vcampus.server.identity;

import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.auth.AuthService;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.registry.IdentityCommandRegistry;
import edu.seu.vcampus.server.identity.service.IdentityService;
import edu.seu.vcampus.server.identity.service.IdentityServiceException;
import edu.seu.vcampus.server.repository.MySqlUserRepository;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 可选真实 MySQL 账号注销申请验证；默认不依赖外部数据库。 */
public final class MySqlAccountCancellationIntegrationTest {
    private static final String PASSWORD = "CancelDb123";
    private JdbcConnectionFactory factory;
    private TransactionManager transactions;
    private IdentityService identity;
    private MySqlUserRepository rootUsers;
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
        hasher = new PasswordHasher(4);
        sessions = new SessionManager();
        identity = IdentityCommandRegistry.createMySqlService(transactions, hasher, sessions);
        rootUsers = new MySqlUserRepository(factory);
        LoginResult result = new AuthService(rootUsers, hasher, sessions).login(
                new LoginRequest("demo_system", "system123"));
        admin = sessions.resolve(result.getSessionToken());
        account = "dbit_cancel_" + Long.toString(System.nanoTime());
    }

    @After public void cleanUp() throws Exception {
        if (sessions != null) sessions.invalidateUserSessions(testUserId);
        if (transactions != null && testUserId > 0L) {
            final long id = testUserId;
            transactions.execute(new TransactionWork<Void>() {
                @Override public Void execute(Connection c) throws Exception {
                    delete(c, "DELETE FROM account_cancellation_requests WHERE user_id=?", id);
                    delete(c, "DELETE FROM user_roles WHERE user_id=?", id);
                    delete(c, "DELETE FROM users WHERE id=?", id);
                    return null;
                }
            });
        }
    }

    @Test public void mysqlRoundTripRejectAndApproveInvalidatesSessions() throws Exception {
        SessionContext student = registerAndLogin();
        AccountCancellationDto first = identity.submitAccountCancellation(student,
                new AccountCancellationRequest("申请注销"));
        assertEquals("PENDING", first.getStatus());
        assertEquals(1L, identity.searchAccountCancellationRequests(admin,
                new AccountCancellationQuery("PENDING", 1, 20)).getTotal());
        try {
            identity.submitAccountCancellation(student, new AccountCancellationRequest("重复申请"));
            fail("duplicate pending request must fail");
        } catch (IdentityServiceException ex) { assertEquals(ResultCodes.CONFLICT, ex.getResultCode()); }
        assertEquals("REJECTED", identity.rejectAccountCancellation(admin,
                new AccountCancellationReviewRequest(first.getId(), "暂不批准")).getStatus());
        AccountCancellationDto secondRequest = identity.submitAccountCancellation(student,
                new AccountCancellationRequest("再次申请"));
        assertEquals("PENDING", secondRequest.getStatus());
        LoginResult another = new AuthService(rootUsers, hasher, sessions).login(
                new LoginRequest(account, PASSWORD));
        SessionContext secondSession = sessions.resolve(another.getSessionToken());
        AccountCancellationDto approved = identity.approveAccountCancellation(admin,
                new AccountCancellationReviewRequest(identity.listOwnAccountCancellations(student,
                        new AccountCancellationQuery("PENDING", 1, 20)).getRequests().get(0).getId(), "确认注销"));
        assertEquals("APPROVED", approved.getStatus());
        assertFalse(rootUsers.findByAccount(account).isEnabled());
        assertFalse(sessions.contains(student.getSessionToken()));
        assertFalse(sessions.contains(secondSession.getSessionToken()));
    }

    @Test public void mysqlRollbackKeepsPendingRequestUserAndSessionActive() throws Exception {
        SessionContext student = registerAndLogin();
        final AccountCancellationDto pending = identity.submitAccountCancellation(student,
                new AccountCancellationRequest("回滚探针"));
        try {
            transactions.execute(new TransactionWork<Void>() {
                @Override public Void execute(Connection c) throws Exception {
                    run(c, "UPDATE account_cancellation_requests SET status='APPROVED', reviewed_by=?, "
                            + "reviewed_at=CURRENT_TIMESTAMP(3), review_remark='rollback' WHERE id=?",
                            admin.getUserId(), pending.getId());
                    run(c, "UPDATE users SET status='DISABLED' WHERE id=?", testUserId);
                    throw new SQLException("cancellation rollback probe");
                }
            });
            fail("rollback probe must fail");
        } catch (SQLException expected) { assertEquals("cancellation rollback probe", expected.getMessage()); }
        assertTrue(rootUsers.findByAccount(account).isEnabled());
        assertTrue(sessions.contains(student.getSessionToken()));
        assertEquals("PENDING", identity.listOwnAccountCancellations(student, null)
                .getRequests().get(0).getStatus());
    }

    private SessionContext registerAndLogin() throws Exception {
        testUserId = identity.register(new RegistrationRequest(account, PASSWORD, "注销集成探针"))
                .getUserId();
        LoginResult result = new AuthService(rootUsers, hasher, sessions).login(
                new LoginRequest(account, PASSWORD));
        return sessions.resolve(result.getSessionToken());
    }

    private static void run(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            ps.executeUpdate();
        }
    }

    private static void delete(Connection c, String sql, long id) throws SQLException { run(c, sql, id); }
}
