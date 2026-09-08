package edu.seu.vcampus.server.library.handler;

import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.library.service.LibraryServiceException;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

import java.io.Serializable;

/** 图书馆命令共享适配器，集中处理请求类型和业务异常映射。 */
public final class LibraryCommandHandler implements CommandHandler {
    private final Permission permission;
    private final Action action;

    public LibraryCommandHandler(Permission permission, Action action) {
        if (action == null) throw new IllegalArgumentException("action is required");
        this.permission = permission;
        this.action = action;
    }

    @Override
    public Message handle(Message request, SessionContext session) {
        try {
            return Message.success(request, action.execute(request.getPayload(), session));
        } catch (LibraryServiceException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getMessage());
        } catch (RuntimeException ex) {
            return Message.failure(request, ResultCodes.INTERNAL_ERROR, "图书馆服务暂时不可用");
        }
    }

    @Override
    public Permission requiredPermission() { return permission; }

    @Override
    public boolean requiresAuthentication() { return true; }

    public static <T> T payload(Serializable payload, Class<T> type) {
        if (!type.isInstance(payload)) {
            throw new LibraryServiceException(ResultCodes.INVALID_INPUT, "请求参数格式不正确");
        }
        return type.cast(payload);
    }

    public interface Action {
        Serializable execute(Serializable payload, SessionContext session);
    }
}
