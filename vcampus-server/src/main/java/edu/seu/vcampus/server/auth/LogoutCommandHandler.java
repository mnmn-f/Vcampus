package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

/** 需要有效会话的登出命令。 */
public final class LogoutCommandHandler implements CommandHandler {
    private final AuthService authService;

    public LogoutCommandHandler(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("authService is required");
        }
        this.authService = authService;
    }

    @Override
    public Message handle(Message request, SessionContext session) {
        authService.logout(session.getSessionToken());
        return Message.success(request, null);
    }

    @Override
    public Permission requiredPermission() {
        return null;
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
