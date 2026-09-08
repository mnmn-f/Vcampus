package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.repository.InMemoryUserRepository;
import edu.seu.vcampus.server.repository.UserRecord;
import edu.seu.vcampus.server.repository.UserRepository;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthServiceTest {
    private InMemoryUserRepository repository;
    private SessionManager sessionManager;
    private AuthService authService;
    private PasswordHasher hasher;

    @Before
    public void setUp() {
        repository = new InMemoryUserRepository();
        sessionManager = new SessionManager();
        hasher = new PasswordHasher(4);
        repository.save(new UserRecord(7L, "multi", hasher.hash("secret"), "多角色用户",
                EnumSet.of(Role.STUDENT, Role.LIBRARIAN), true));
        authService = new AuthService(repository, hasher, sessionManager);
    }

    @Test
    public void loginReturnsAllRolesAndServerSession() throws Exception {
        LoginResult result = authService.login(new LoginRequest("multi", "secret"));

        assertEquals(7L, result.getUserId());
        assertEquals(EnumSet.of(Role.STUDENT, Role.LIBRARIAN), result.getRoles());
        assertTrue(result.getRoles().contains(result.getActiveRole()));
        assertTrue(sessionManager.contains(result.getSessionToken()));
    }

    @Test
    public void loginUsesRepositoryResolutionForNonUsernameIdentifier() throws Exception {
        final UserRecord user = new UserRecord(11L, "campus-account", hasher.hash("secret"),
                "学号用户", Role.STUDENT, true);
        UserRepository identifiers = new UserRepository() {
            @Override public UserRecord findByAccount(String value) {
                return "S2026001".equals(value) ? user : null;
            }
            @Override public UserRecord findById(long id) {
                return id == user.getUserId() ? user : null;
            }
            @Override public void save(UserRecord ignored) { }
        };
        LoginResult result = new AuthService(identifiers, hasher, sessionManager)
                .login("S2026001", "secret");
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getDisplayName(), result.getDisplayName());
    }

    @Test
    public void invalidPasswordDoesNotCreateSession() throws Exception {
        try {
            authService.login("multi", "wrong");
        } catch (AuthenticationException ex) {
            assertEquals(ResultCodes.INVALID_CREDENTIALS, ex.getResultCode());
            assertEquals(0, sessionManager.activeSessionCount());
            return;
        }
        throw new AssertionError("invalid password should fail");
    }

    @Test
    public void disabledAccountIsReportedSeparately() throws Exception {
        repository.save(new UserRecord(8L, "disabled", hasher.hash("secret"), "停用用户",
                Role.STUDENT, false));

        try {
            authService.login("disabled", "secret");
        } catch (AuthenticationException ex) {
            assertEquals(ResultCodes.ACCOUNT_DISABLED, ex.getResultCode());
            return;
        }
        throw new AssertionError("disabled account should fail");
    }

    @Test
    public void logoutRemovesOnlyTheRequestedSession() throws Exception {
        LoginResult first = authService.login("multi", "secret");
        LoginResult second = authService.login("multi", "secret");

        assertTrue(authService.logout(first.getSessionToken()));
        assertFalse(sessionManager.contains(first.getSessionToken()));
        assertTrue(sessionManager.contains(second.getSessionToken()));
        assertFalse(authService.logout(first.getSessionToken()));
        assertNotNull(second.getSessionToken());
    }

    @Test
    public void switchingRoleUpdatesTrustedServerSession() throws Exception {
        LoginResult result = authService.login("multi", "secret");

        assertEquals(Role.LIBRARIAN,
                authService.switchRole(result.getSessionToken(), Role.LIBRARIAN));
        assertEquals(Role.LIBRARIAN, sessionManager.find(result.getSessionToken())
                .getActiveRole());
        try {
            authService.switchRole(result.getSessionToken(), Role.DORM_MANAGER);
        } catch (AuthenticationException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
            return;
        }
        throw new AssertionError("unassigned role should fail");
    }
}
