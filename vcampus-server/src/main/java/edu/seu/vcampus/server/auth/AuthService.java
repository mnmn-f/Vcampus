package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.repository.UserRecord;
import edu.seu.vcampus.server.repository.UserRepository;
import edu.seu.vcampus.server.security.PasswordHasher;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.util.Set;

/** 登录、登出和登录会话创建服务。 */
public final class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SessionManager sessionManager;
    private final LoginAuditSink loginAuditSink;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher,
                       SessionManager sessionManager) {
        this(userRepository, passwordHasher, sessionManager, LoginAuditSink.NOOP);
    }

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher,
                       SessionManager sessionManager, LoginAuditSink loginAuditSink) {
        if (userRepository == null || passwordHasher == null || sessionManager == null) {
            throw new IllegalArgumentException("authentication dependencies are required");
        }
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.sessionManager = sessionManager;
        this.loginAuditSink = loginAuditSink == null ? LoginAuditSink.NOOP : loginAuditSink;
    }

    public LoginResult login(LoginRequest request) throws AuthenticationException {
        if (request == null) {
            audit(null, null, null, ResultCodes.INVALID_INPUT, null);
            throw invalidInput();
        }
        return login(request.getAccount(), request.getPassword());
    }

    public LoginResult login(String account, String password)
            throws AuthenticationException {
        return login(account, password, null);
    }

    public LoginResult login(String account, String password, String clientIp)
            throws AuthenticationException {
        if (isBlank(account) || isBlank(password)) {
            audit(null, account, null, ResultCodes.INVALID_INPUT, clientIp);
            throw invalidInput();
        }
        UserRecord found = userRepository.findByAccount(account.trim());
        if (found == null) {
            audit(null, account.trim(), null, ResultCodes.INVALID_CREDENTIALS, clientIp);
            throw invalidCredentials();
        }
        UserRecord user = found;
        if (!user.isEnabled()) {
            audit(user, user.getAccount(), null, ResultCodes.ACCOUNT_DISABLED, clientIp);
            throw new AuthenticationException(ResultCodes.ACCOUNT_DISABLED,
                    "账号已停用，请联系系统管理员");
        }
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            audit(user, user.getAccount(), null, ResultCodes.INVALID_CREDENTIALS, clientIp);
            throw invalidCredentials();
        }
        Set<Role> roles = user.getRoles();
        SessionContext session = sessionManager.createSession(
                user.getUserId(), user.getAccount(), user.getDisplayName(), roles);
        audit(user, user.getAccount(), session.getActiveRole(), ResultCodes.OK, clientIp);
        return new LoginResult(user.getUserId(), user.getAccount(),
                user.getDisplayName(), roles, session.getActiveRole(),
                session.getSessionToken());
    }

    public boolean logout(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return false;
        }
        boolean existed = sessionManager.contains(sessionToken);
        sessionManager.invalidate(sessionToken);
        return existed;
    }

    public Role switchRole(String sessionToken, Role role)
            throws AuthenticationException {
        if (isBlank(sessionToken) || role == null) {
            throw new AuthenticationException(ResultCodes.INVALID_INPUT,
                    "请选择有效职责");
        }
        if (!sessionManager.switchActiveRole(sessionToken, role)) {
            throw new AuthenticationException(ResultCodes.FORBIDDEN,
                    "当前账号未被授予该职责");
        }
        return role;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    private void audit(UserRecord user, String account, Role role, String resultCode,
                       String clientIp) {
        try {
            loginAuditSink.record(user == null ? null : Long.valueOf(user.getUserId()),
                    account, role, resultCode, clientIp);
        } catch (RuntimeException ignored) {
            // 审计故障不应把认证结果变成未知；生产实现应自行记录监控告警。
        }
    }

    private static AuthenticationException invalidInput() {
        return new AuthenticationException(ResultCodes.INVALID_INPUT,
                "账号和密码不能为空");
    }

    private static AuthenticationException invalidCredentials() {
        return new AuthenticationException(ResultCodes.INVALID_CREDENTIALS,
                "账号或密码错误");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
