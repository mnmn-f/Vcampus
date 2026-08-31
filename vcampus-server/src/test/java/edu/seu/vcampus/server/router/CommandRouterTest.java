package edu.seu.vcampus.server.router;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Commands;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.ServerMain;
import edu.seu.vcampus.server.repository.InMemoryUserRepository;
import edu.seu.vcampus.server.repository.UserRecord;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandRouterTest {
    private PasswordHasher hasher;
    private SessionManager sessionManager;
    private CommandRouter router;

    @Before
    public void setUp() {
        hasher = new PasswordHasher(4);
        sessionManager = new SessionManager();
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.save(new UserRecord(1L, "student", hasher.hash("secret"), "学生",
                Role.STUDENT, true));
        repository.save(new UserRecord(2L, "academic", hasher.hash("secret"), "教务",
                Role.ACADEMIC_ADMIN, true));
        router = ServerMain.createRouter(repository, hasher, sessionManager);
    }

    @Test
    public void loginAndUnknownCommandReturnCorrelatedResponses() {
        Message login = router.route(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest("student", "secret")));

        assertTrue(login.isSuccess());
        assertEquals(Commands.AUTH_LOGIN, login.getCommand());
        assertTrue(login.getPayload() instanceof LoginResult);

        Message unknown = router.route(Message.request("does.not.exist", null, null));
        assertFalse(unknown.isSuccess());
        assertEquals("COMMON.UNKNOWN_COMMAND", unknown.getResultCode());
        assertEquals("does.not.exist", unknown.getCommand());
    }

    @Test
    public void protectedCommandUsesSessionRolesInsteadOfPayloadIdentity() {
        router.register("course.manage", new CommandHandler() {
            @Override
            public Message handle(Message request,
                                   edu.seu.vcampus.server.security.SessionContext session) {
                return Message.success(request, null);
            }

            @Override
            public Permission requiredPermission() {
                return Permission.COURSE_MANAGE;
            }

            @Override
            public boolean requiresAuthentication() {
                return true;
            }
        });

        Message studentLogin = router.route(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest("student", "secret")));
        LoginResult student = (LoginResult) studentLogin.getPayload();
        Message forbidden = Message.request("course.manage", student.getSessionToken(),
                Long.valueOf(2L));
        Message forbiddenResponse = router.route(forbidden);
        assertFalse(forbiddenResponse.isSuccess());
        assertEquals(ResultCodes.FORBIDDEN, forbiddenResponse.getResultCode());

        Message academicLogin = router.route(Message.request(Commands.AUTH_LOGIN, null,
                new LoginRequest("academic", "secret")));
        LoginResult academic = (LoginResult) academicLogin.getPayload();
        Message allowed = router.route(Message.request("course.manage",
                academic.getSessionToken(), Long.valueOf(1L)));
        assertTrue(allowed.isSuccess());
    }

    @Test
    public void missingOrInvalidSessionIsUnauthorized() {
        router.register("protected", new CommandHandler() {
            @Override
            public Message handle(Message request,
                                   edu.seu.vcampus.server.security.SessionContext session) {
                return Message.success(request, null);
            }

            @Override
            public Permission requiredPermission() {
                return Permission.PROFILE_READ;
            }

            @Override
            public boolean requiresAuthentication() {
                return true;
            }
        });

        Message missing = router.route(Message.request("protected", null, null));
        Message invalid = router.route(Message.request("protected", "stale-token", null));
        assertEquals(ResultCodes.UNAUTHORIZED, missing.getResultCode());
        assertEquals(ResultCodes.UNAUTHORIZED, invalid.getResultCode());
    }

    @Test
    public void handlerExceptionBecomesGenericInternalError() {
        router.register("broken", new CommandHandler() {
            @Override
            public Message handle(Message request,
                                   edu.seu.vcampus.server.security.SessionContext session) {
                throw new IllegalStateException("database details must not leak");
            }

            @Override
            public Permission requiredPermission() {
                return null;
            }

            @Override
            public boolean requiresAuthentication() {
                return false;
            }
        });

        Message response = router.route(Message.request("broken", null, null));
        assertFalse(response.isSuccess());
        assertEquals(ResultCodes.INTERNAL_ERROR, response.getResultCode());
    }
}
