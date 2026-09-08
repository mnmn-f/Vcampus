package edu.seu.vcampus.server.ai.handler;

import edu.seu.vcampus.common.ai.*;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AiCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.ai.service.AiAssistantService;
import edu.seu.vcampus.server.ai.service.AiConversationService;
import edu.seu.vcampus.server.ai.service.AiKnowledgeService;
import edu.seu.vcampus.server.ai.service.AiFeedbackService;
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
    private final AiFeedbackService feedback;

    public AiCommandHandler(String command, Permission permission, AiAssistantService assistant,
            AiConversationService conversations, AiKnowledgeService knowledge) {
        this(command, permission, assistant, conversations, knowledge, null);
    }

    public AiCommandHandler(String command, Permission permission, AiAssistantService assistant,
            AiConversationService conversations, AiKnowledgeService knowledge,
            AiFeedbackService feedback) {
        this.command = command; this.permission = permission; this.assistant = assistant;
        this.conversations = conversations; this.knowledge = knowledge; this.feedback = feedback;
    }

    public Message handle(Message request, SessionContext session) {
        try { return Message.success(request, execute(request.getPayload(), session)); }
        catch (AiServiceException ex) { return Message.failure(request, ex.getResultCode(), ex.getMessage()); }
        catch (RuntimeException ex) { return Message.failure(request, ResultCodes.INVALID_INPUT, "AI 请求参数不正确"); }
    }

    private Serializable execute(Serializable payload, SessionContext session) {
        if (AiCommands.PING.equals(command)) return Boolean.TRUE;
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
        if (AiCommands.SESSION_ARCHIVED.equals(command)) {
            return new ArrayList<AiSessionSummary>(conversations.archived(session));
        }
        if (AiCommands.SESSION_HISTORY.equals(command)) {
            return new ArrayList<AiChatMessage>(conversations.history(session,
                    required(payload, AiSessionRequest.class).getSessionId()));
        }
        if (AiCommands.SESSION_CLEAR.equals(command)) {
            conversations.clear(session, required(payload, AiSessionRequest.class).getSessionId());
            return Boolean.TRUE;
        }
        if (AiCommands.SESSION_RESTORE.equals(command)) {
            conversations.restore(session, required(payload, AiSessionRequest.class).getSessionId());
            return Boolean.TRUE;
        }
        if (AiCommands.SESSION_RENAME.equals(command)) {
            AiSessionRenameRequest value = required(payload, AiSessionRenameRequest.class);
            conversations.rename(session, value.getSessionId(), value.getTitle());
            return Boolean.TRUE;
        }
        if (AiCommands.KNOWLEDGE_LIST.equals(command)) {
            return knowledge.search(payload == null ? new AiKnowledgeQuery()
                    : required(payload, AiKnowledgeQuery.class));
        }
        if (AiCommands.KNOWLEDGE_SAVE.equals(command)) {
            return knowledge.save(session, required(payload, AiKnowledgeSaveRequest.class));
        }
        if (AiCommands.KNOWLEDGE_IMPORT.equals(command)) {
            return knowledge.importBatch(session, required(payload, AiKnowledgeBatchRequest.class));
        }
        if (AiCommands.KNOWLEDGE_DELETE.equals(command)) {
            knowledge.delete(session, required(payload, AiKnowledgeDeleteRequest.class).getChunkId());
            return Boolean.TRUE;
        }
        if (AiCommands.KNOWLEDGE_TEST.equals(command)) {
            return assistant.testKnowledge(required(payload, AiKnowledgeTestRequest.class).getQuestion());
        }
        if (AiCommands.KNOWLEDGE_VERSIONS.equals(command)) {
            return new ArrayList<AiKnowledgeVersion>(knowledge.versions(
                    required(payload, AiKnowledgeVersionRequest.class).getChunkId()));
        }
        if (AiCommands.KNOWLEDGE_ROLLBACK.equals(command)) {
            AiKnowledgeVersionRequest value = required(payload, AiKnowledgeVersionRequest.class);
            if (value.getVersionId() == null) throw new IllegalArgumentException("version required");
            return knowledge.rollback(session, value.getChunkId(), value.getVersionId().longValue());
        }
        if (AiCommands.FEEDBACK_SAVE.equals(command)) {
            requireFeedback().save(session, required(payload, AiFeedbackRequest.class));
            return Boolean.TRUE;
        }
        if (AiCommands.FEEDBACK_LIST.equals(command)) {
            return new ArrayList<AiFeedbackEntry>(requireFeedback().latest(payload == null
                    ? new AiFeedbackQuery(null, null, null, null, null)
                    : required(payload, AiFeedbackQuery.class)));
        }
        if (AiCommands.FEEDBACK_UPDATE.equals(command)) {
            requireFeedback().update(session, required(payload, AiFeedbackTriageRequest.class));
            return Boolean.TRUE;
        }
        if (AiCommands.TOOL_ROUTE_TEST.equals(command)) {
            return assistant.testToolRoute(required(payload, AiToolRouteTestRequest.class).getQuestion());
        }
        if (AiCommands.TOOL_STATUS.equals(command)) {
            return new ArrayList<AiToolStatus>(assistant.toolStatuses());
        }
        if (AiCommands.MONITOR.equals(command)) return assistant.monitor();
        throw new IllegalArgumentException("unsupported AI command");
    }

    private AiFeedbackService requireFeedback() {
        if (feedback == null) throw new IllegalStateException("feedback service unavailable");
        return feedback;
    }

    private <T> T required(Serializable payload, Class<T> type) {
        if (!type.isInstance(payload)) throw new IllegalArgumentException("invalid payload");
        return type.cast(payload);
    }

    public Permission requiredPermission() { return permission; }
    public boolean requiresAuthentication() { return true; }
}
