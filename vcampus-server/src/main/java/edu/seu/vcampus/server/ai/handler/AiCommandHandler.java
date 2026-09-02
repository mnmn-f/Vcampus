package edu.seu.vcampus.server.ai.handler;

import edu.seu.vcampus.common.ai.*;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AiCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.ai.service.AiAssistantService;
import edu.seu.vcampus.server.ai.service.AiConversationService;
import edu.seu.vcampus.server.ai.service.AiKnowledgeService;
import edu.seu.vcampus.server.ai.service.AiServiceException;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

import java.io.Serializable;
import java.util.ArrayList;

/** AI 非流式管理命令的共享适配器。 */
public final class AiCommandHandler implements CommandHandler {
    private final String command;
    private final Permission permission;
    private final AiAssistantService assistant;
    private final AiConversationService conversations;
    private final AiKnowledgeService knowledge;

    public AiCommandHandler(String command, Permission permission, AiAssistantService assistant,
            AiConversationService conversations, AiKnowledgeService knowledge) {
        this.command = command; this.permission = permission; this.assistant = assistant;
        this.conversations = conversations; this.knowledge = knowledge;
    }

    public Message handle(Message request, SessionContext session) {
        try { return Message.success(request, execute(request.getPayload(), session)); }
        catch (AiServiceException ex) { return Message.failure(request, ex.getResultCode(), ex.getMessage()); }
        catch (RuntimeException ex) { return Message.failure(request, ResultCodes.INVALID_INPUT, "AI 请求参数不正确"); }
    }

    private Serializable execute(Serializable payload, SessionContext session) {
        if (AiCommands.CANCEL.equals(command)) {
            return Boolean.valueOf(assistant.cancel(session,
                    required(payload, AiCancelRequest.class).getRequestId()));
        }
        if (AiCommands.CONFIRM.equals(command)) {
            return assistant.confirm(session, required(payload, AiActionConfirmation.class));
        }
        if (AiCommands.SESSION_CREATE.equals(command)) return conversations.create(session);
        if (AiCommands.SESSION_LIST.equals(command)) {
            return new ArrayList<AiSessionSummary>(conversations.list(session));
        }
        if (AiCommands.SESSION_HISTORY.equals(command)) {
            return new ArrayList<AiChatMessage>(conversations.history(session,
                    required(payload, AiSessionRequest.class).getSessionId()));
        }
        if (AiCommands.SESSION_CLEAR.equals(command)) {
            conversations.clear(session, required(payload, AiSessionRequest.class).getSessionId());
            return Boolean.TRUE;
        }
        if (AiCommands.KNOWLEDGE_LIST.equals(command)) {
            return knowledge.search(payload == null ? new AiKnowledgeQuery()
                    : required(payload, AiKnowledgeQuery.class));
        }
        if (AiCommands.KNOWLEDGE_SAVE.equals(command)) {
            return knowledge.save(session, required(payload, AiKnowledgeSaveRequest.class));
        }
        if (AiCommands.KNOWLEDGE_DELETE.equals(command)) {
            knowledge.delete(session, required(payload, AiKnowledgeDeleteRequest.class).getChunkId());
            return Boolean.TRUE;
        }
        if (AiCommands.MONITOR.equals(command)) return assistant.monitor();
        throw new IllegalArgumentException("unsupported AI command");
    }

    private <T> T required(Serializable payload, Class<T> type) {
        if (!type.isInstance(payload)) throw new IllegalArgumentException("invalid payload");
        return type.cast(payload);
    }

    public Permission requiredPermission() { return permission; }
    public boolean requiresAuthentication() { return true; }
}
