package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.dto.auth.LoginRequest;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

/** 将登录协议请求适配到认证服务。 */
public final class LoginCommandHandler implements CommandHandler {
    private final AuthService authService;

    public LoginCommandHandler(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("authService is required");
        }
        this.authService = authService;
    }

    @Override
    public Message handle(Message request, SessionContext session) {
        if (!(request.getPayload() instanceof LoginRequest)) {
            return Message.failure(request, ResultCodes.INVALID_INPUT, "登录请求格式不正确");
        }
        try {
            LoginResult result = authService.login((LoginRequest) request.getPayload());
            return Message.success(request, result);
        } catch (AuthenticationException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        }
    }

    @Override
    public Permission requiredPermission() {
        return null;
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }
}
