package edu.seu.vcampus.server.router;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.MessageType;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.protocol.ServerResultCodes;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 统一命令登记、会话解析、细粒度鉴权和异常兜底入口。 */
public final class CommandRouter {
    private static final Logger LOGGER = Logger.getLogger(CommandRouter.class.getName());
    private final Map<String, CommandHandler> handlers =
            new ConcurrentHashMap<String, CommandHandler>();
    private final SessionManager sessionManager;

    public CommandRouter() {
        this(new SessionManager());
    }

    public CommandRouter(SessionManager sessionManager) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("sessionManager is required");
        }
        this.sessionManager = sessionManager;
    }

    public CommandRouter register(String command, CommandHandler handler) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("command is required");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler is required");
        }
        handlers.put(command.trim(), handler);
        return this;
    }

    public Message route(Message request) {
        if (request == null) {
            return null;
        }
        if (request.getType() != MessageType.REQUEST
                || request.getCommand() == null
                || request.getCommand().trim().isEmpty()) {
            return Message.failure(request, ServerResultCodes.MALFORMED_REQUEST,
                    "请求格式不正确");
        }
        String command = request.getCommand().trim();
        CommandHandler handler = handlers.get(command);
        if (handler == null) {
            return Message.failure(request, ServerResultCodes.UNKNOWN_COMMAND,
                    "不支持的操作");
        }

        try {
            SessionContext session = authorize(request, handler);
            Message response = handler.handle(request, session);
            return response == null
                    ? Message.failure(request, ResultCodes.INTERNAL_ERROR, "服务处理失败")
                    : response;
        } catch (RouteException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Command failed: " + command, ex);
            return Message.failure(request, ResultCodes.INTERNAL_ERROR, "服务器暂时无法处理请求");
        }
    }

    public boolean isStreamingCommand(String command) {
        if (command == null) return false;
        return handlers.get(command.trim()) instanceof StreamingCommandHandler;
    }

    public void routeStream(Message request, StreamWriter writer) {
        if (writer == null) throw new IllegalArgumentException("writer is required");
        if (request == null || request.getType() != MessageType.REQUEST
                || request.getCommand() == null
                || request.getCommand().trim().isEmpty()) {
            if (request != null) writer.write(Message.failure(request,
                    ServerResultCodes.MALFORMED_REQUEST, "请求格式不正确"));
            return;
        }
        CommandHandler handler = handlers.get(request.getCommand().trim());
        if (!(handler instanceof StreamingCommandHandler)) {
            writer.write(Message.failure(request, ServerResultCodes.UNKNOWN_COMMAND,
                    "不支持的流式操作"));
            return;
        }
        try {
            SessionContext session = authorize(request, handler);
            ((StreamingCommandHandler) handler).handleStream(request, session, writer);
        } catch (RouteException ex) {
            writer.write(Message.failure(request, ex.getResultCode(), ex.getUserMessage()));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Streaming command failed: "
                    + request.getCommand().trim(), ex);
            writer.write(Message.failure(request, ResultCodes.INTERNAL_ERROR,
                    "服务器暂时无法处理请求"));
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public int registeredCommandCount() {
        return handlers.size();
    }

    public boolean isRegistered(String command) {
        return command != null && handlers.containsKey(command.trim());
    }

    private SessionContext authorize(Message request, CommandHandler handler)
            throws RouteException {
        Permission required = handler.requiredPermission();
        if (!handler.requiresAuthentication() && required == null) {
            return null;
        }
        String token = request.getSessionToken();
        SessionContext found = sessionManager.resolve(token);
        if (found == null) {
            throw new RouteException(ResultCodes.UNAUTHORIZED, "请先登录");
        }
        SessionContext session = found;
        if (required != null && !session.allows(required)) {
            throw new RouteException(ResultCodes.FORBIDDEN, "当前角色无权执行此操作");
        }
        return session;
    }

    private static final class RouteException extends Exception {
        private static final long serialVersionUID = 1L;
        private final String resultCode;

        private RouteException(String resultCode, String userMessage) {
            super(userMessage);
            this.resultCode = resultCode;
        }

        private String getResultCode() {
            return resultCode;
        }

        private String getUserMessage() {
            return getMessage();
        }
    }
}
