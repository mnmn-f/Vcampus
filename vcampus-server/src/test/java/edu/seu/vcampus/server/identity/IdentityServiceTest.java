package edu.seu.vcampus.server.identity;

import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.identity.PasswordChangeRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.identity.RoleRevokeRequest;
import edu.seu.vcampus.common.dto.identity.SessionDto;
import edu.seu.vcampus.common.dto.identity.SessionRevokeRequest;
import edu.seu.vcampus.common.dto.identity.UserStatusUpdateRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.auth.AuthService;
import edu.seu.vcampus.server.auth.AuthenticationException;
import edu.seu.vcampus.server.auth.LoginAuditSink;
import edu.seu.vcampus.server.identity.registry.IdentityCommandRegistry;
import edu.seu.vcampus.server.identity.repository.InMemoryIdentityRepository;
import edu.seu.vcampus.server.identity.service.IdentityService;
import edu.seu.vcampus.server.identity.service.IdentityServiceException;
import edu.seu.vcampus.server.identity.service.InMemoryIdentityTransactionRunner;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 身份真实纵切：资料、权限、审计和同一运行时会话源。 */
public final class IdentityServiceTest {
    private InMemoryIdentityRepository repository;
    private PasswordHasher hasher;
    private SessionManager sessions;
    private IdentityService service;
    private SessionContext admin;

    @Before public void setUp() throws Exception {
        repository = new InMemoryIdentityRepository();
        hasher = new PasswordHasher(4);
        sessions = repository.getSessionManager();
        service = new IdentityService(repository, hasher, sessions,
                new InMemoryIdentityTransactionRunner(repository));
        repository.addUser(1L, "admin", hasher.hash("Admin1234"), "系统管理员", Role.SYSTEM_ADMIN);
        repository.addUser(2L, "target", hasher.hash("Target1234"), "目标用户", Role.STUDENT);
        admin = login("admin", "Admin1234").context;
    }

    @Test public void registrationValidatesAndAssignsStudent() throws Exception {
        assertEquals(Role.STUDENT, service.register(new RegistrationRequest(
                "new_user", "NewUser123", "新用户")).getRoles().iterator().next());
        try {
            service.register(new RegistrationRequest("new_user", "NewUser123", "新用户"));
            fail("duplicate account must fail");
        } catch (IdentityServiceException ex) { assertEquals(ResultCodes.CONFLICT, ex.getResultCode()); }
        try {
            service.register(new RegistrationRequest("weak_user", "123", "弱密码"));
            fail("weak password must fail");
        } catch (IdentityServiceException ex) { assertEquals(ResultCodes.INVALID_INPUT, ex.getResultCode()); }
    }

    @Test public void activeRoleIsTheOnlyPermissionSource() throws Exception {
        SessionContext studentView = new SessionContext("mixed", 1L, "admin", "系统管理员",
                EnumSet.of(Role.SYSTEM_ADMIN, Role.STUDENT), Role.STUDENT);
        try {
            service.searchUsers(studentView, null);
            fail("student active role must not inherit admin permission");
        } catch (IdentityServiceException ex) { assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode()); }
    }

    @Test public void passwordAndSelfLockoutRulesApply() throws Exception {
        SessionContext target = login("target", "Target1234").context;
        service.changePassword(target, new PasswordChangeRequest("Target1234", "Target5678"));
        try {
            service.updateUserStatus(admin, new UserStatusUpdateRequest(1L, "DISABLED"));
            fail("admin cannot disable itself");
        } catch (IdentityServiceException ex) { assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode()); }
        try {
            service.revokeRole(admin, new RoleRevokeRequest(2L, Role.STUDENT));
            fail("last role must be retained");
        } catch (IdentityServiceException ex) { assertEquals(ResultCodes.CONFLICT, ex.getResultCode()); }
    }

    @Test public void loginSessionCanBeListedAndRevokedWithoutObservationHook() throws Exception {
        Login login = login("target", "Target1234");
        SessionDto targetSession = null;
        for (SessionDto value : service.searchSessions(admin, null).getItems()) {
            if (value.getUserId() == 2L) targetSession = value;
        }
        assertTrue(targetSession != null);
        assertTrue(service.revokeSession(admin, new SessionRevokeRequest(targetSession.getId())));
        CommandRouter router = IdentityCommandRegistry.registerAll(new CommandRouter(sessions), service);
        Message response = router.route(Message.request(IdentityCommands.PROFILE_SELF,
                login.result.getSessionToken(), null));
        assertFalse(response.isSuccess());
        assertEquals(ResultCodes.UNAUTHORIZED, response.getResultCode());
    }

    @Test public void authEmitsSuccessAndFailureLoginAudits() throws Exception {
        final List<String> outcomes = new ArrayList<String>();
        LoginAuditSink sink = new LoginAuditSink() {
            @Override public void record(Long userId, String account, Role role, String result,
                                         String clientIp) { outcomes.add(result); }
        };
        AuthService auth = new AuthService(repository.getAuthRepository(), hasher, sessions, sink);
        auth.login("target", "Target1234");
        try { auth.login("target", "bad-password"); } catch (AuthenticationException expected) { }
        assertEquals(2, outcomes.size());
        assertEquals(ResultCodes.OK, outcomes.get(0));
        assertEquals(ResultCodes.INVALID_CREDENTIALS, outcomes.get(1));
    }

    private Login login(String account, String password) throws Exception {
        AuthService auth = new AuthService(repository.getAuthRepository(), hasher, sessions);
        LoginResult result = auth.login(account, password);
        return new Login(result, sessions.resolve(result.getSessionToken()));
    }

    private static final class Login {
        private final LoginResult result;
        private final SessionContext context;
        private Login(LoginResult result, SessionContext context) {
            this.result = result;
            this.context = context;
        }
    }
}
