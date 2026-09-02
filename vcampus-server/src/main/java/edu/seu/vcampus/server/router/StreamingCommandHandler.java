package edu.seu.vcampus.server.router;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.server.security.SessionContext;

/** 一次请求可以产生多个响应事件的命令处理器。 */
public interface StreamingCommandHandler extends CommandHandler {
    void handleStream(Message request, SessionContext session, StreamWriter writer);
}
