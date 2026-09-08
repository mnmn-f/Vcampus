package edu.seu.vcampus.server.router;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.security.SessionContext;

/** 一个命令的服务端处理边界。 */
public interface CommandHandler {
    Message handle(Message request, SessionContext session);

    /** 返回非空权限时，路由器会在调用处理器前完成会话与权限校验。 */
    Permission requiredPermission();

    /** 无业务权限但必须登录的命令（例如登出）可覆盖此方法。 */
    boolean requiresAuthentication();
}
