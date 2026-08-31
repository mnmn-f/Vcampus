package edu.seu.vcampus.server;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.repository.MySqlUserRepository;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 真实组合根冒烟：同一连接工厂/会话管理器贯通认证、身份和业务命令。 */
public final class ProductionRouterMySqlIntegrationTest {
    @Test public void loginProfileBusinessLogoutAndOldTokenAreConnected() {
        Assume.assumeTrue(Boolean.getBoolean("vcampus.mysql.integration"));
        JdbcConnectionFactory factory = new JdbcConnectionFactory(System.getProperty(
                "vcampus.db.url", JdbcConnectionFactory.DEFAULT_URL), System.getProperty(
                "vcampus.db.user", JdbcConnectionFactory.DEFAULT_USER), System.getProperty(
                "vcampus.db.password", JdbcConnectionFactory.DEFAULT_PASSWORD));
        SessionManager sessions = new SessionManager();
        CommandRouter router = ServerMain.createProductionRouter(new MySqlUserRepository(factory),
                new PasswordHasher(4), sessions, factory);

        Message login = router.route(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest("demo_student", "student123")));
        assertTrue(login.isSuccess());
        LoginResult result = (LoginResult) login.getPayload();
        String token = result.getSessionToken();
        assertTrue(sessions.contains(token));

        Message profile = router.route(Message.request(IdentityCommands.PROFILE_SELF, token, null));
        assertTrue(profile.isSuccess());
        Message courses = router.route(Message.request(AcademicCommands.COURSE_LIST, token, null));
        assertTrue(courses.isSuccess());

        Message logout = router.route(Message.request(Commands.AUTH_LOGOUT, token, null));
        assertTrue(logout.isSuccess());
        Message oldToken = router.route(Message.request(IdentityCommands.PROFILE_SELF, token, null));
        assertFalse(oldToken.isSuccess());
        assertEquals(ResultCodes.UNAUTHORIZED, oldToken.getResultCode());
    }
}
