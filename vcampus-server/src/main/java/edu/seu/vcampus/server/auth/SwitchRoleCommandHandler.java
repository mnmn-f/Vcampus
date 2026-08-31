package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.dto.auth.SwitchRoleRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

/** 在服务端可信会话中切换当前职责。 */
public final class SwitchRoleCommandHandler implements CommandHandler {
    private final AuthService authService;

    public SwitchRoleCommandHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Message handle(Message request, SessionContext session) {
        if (!(request.getPayload() instanceof SwitchRoleRequest)) {
            return Message.failure(request, ResultCodes.INVALID_INPUT, "请选择有效职责");
        }
        try {
            Role role = authService.switchRole(session.getSessionToken(),
                    ((SwitchRoleRequest) request.getPayload()).getRole());
            return Message.success(request, role);
        } catch (AuthenticationException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getMessage());
        }
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
