package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.ai.AiActionConfirmation;
import edu.seu.vcampus.common.ai.AiConfirmResult;
import edu.seu.vcampus.common.ai.AiKnowledgeChunk;
import edu.seu.vcampus.common.ai.AiMonitorSnapshot;
import edu.seu.vcampus.common.ai.AiPendingAction;
import edu.seu.vcampus.common.ai.AiQuery;
import edu.seu.vcampus.common.ai.AiStreamChunk;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.model.AiModel;
import edu.seu.vcampus.server.ai.model.AiTextSink;
import edu.seu.vcampus.server.ai.repository.AiToolRecord;
import edu.seu.vcampus.server.ai.tool.AiTool;
import edu.seu.vcampus.server.ai.tool.AiToolInvocation;
import edu.seu.vcampus.server.ai.tool.AiToolRegistry;
import edu.seu.vcampus.server.ai.tool.ToolBridge;
import edu.seu.vcampus.server.ai.tool.ToolIntentParser;
import edu.seu.vcampus.server.ai.tool.ToolResultFormatter;
import edu.seu.vcampus.server.router.StreamWriter;
import edu.seu.vcampus.server.security.SessionContext;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** AI 问答、RAG、工具确认和降级策略的编排入口。 */
public final class AiAssistantService {
    private final AiConversationService conversations;
    private final AiKnowledgeService knowledge;
    private final AiToolService toolLogs;
    private final AiToolRegistry tools;
    private final ToolBridge bridge;
    private final ToolIntentParser intents = new ToolIntentParser();
    private final ToolResultFormatter formatter = new ToolResultFormatter();
    private final AiPromptBuilder prompts = new AiPromptBuilder();
    private final AiModel model;
    private final ConcurrentMap<String, ActiveQuery> active =
            new ConcurrentHashMap<String, ActiveQuery>();

    public AiAssistantService(AiConversationService conversations, AiKnowledgeService knowledge,
            AiToolService toolLogs, AiToolRegistry tools, ToolBridge bridge, AiModel model) {
        this.conversations = conversations; this.knowledge = knowledge; this.toolLogs = toolLogs;
        this.tools = tools; this.bridge = bridge; this.model = model;
    }

    public void query(final Message envelope, final AiQuery query,
                      final SessionContext session, final StreamWriter writer) {
        validate(query);
        ActiveQuery running = new ActiveQuery(Thread.currentThread(), session.getUserId());
        if (active.putIfAbsent(query.getRequestId(), running) != null) {
            throw new AiServiceException(ResultCodes.CONFLICT, "请求编号正在使用");
        }
        String sessionId = null;
        try {
            sessionId = conversations.beginQuery(session, query.getSessionId(),
                    query.getRequestId(), query.getText());
            AiToolInvocation invocation = intents.parse(query.getText());
            if (invocation != null) {
                handleTool(envelope, query, sessionId, session, writer, invocation);
            } else {
                answer(envelope, query, sessionId, writer);
            }
        } catch (AiServiceException ex) {
            writer.write(Message.failure(envelope, ex.getResultCode(), ex.getMessage()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (sessionId != null) conversations.saveAssistant(sessionId, query.getRequestId(),
                    "回答已取消。", "CANCELLED");
            writer.write(Message.failure(envelope, ResultCodes.CONFLICT, "回答已取消"));
        } catch (Exception ex) {
            writer.write(Message.failure(envelope, ResultCodes.INTERNAL_ERROR,
                    "校园助手暂时无法回答，请稍后重试"));
        } finally { active.remove(query.getRequestId()); }
    }

    public boolean cancel(SessionContext session, String requestId) {
        ActiveQuery task = requestId == null ? null : active.get(requestId);
        if (task == null || task.userId != session.getUserId()) return false;
        task.thread.interrupt(); return true;
    }

    public AiConfirmResult confirm(SessionContext session, AiActionConfirmation confirmation) {
        if (confirmation == null || confirmation.getActionId() <= 0) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "确认参数不正确");
        }
        AiToolRecord record = toolLogs.pending(confirmation.getActionId(), session.getUserId());
        if (!confirmation.isAgreed()) {
            toolLogs.finish(record.getId(), "CANCELLED", "用户取消", Long.valueOf(session.getUserId()));
            String text = "已取消：" + record.getToolName();
            conversations.saveAssistant(record.getSessionId(), record.getRequestId(), text, "COMPLETED");
            return new AiConfirmResult(record.getSessionId(), text);
        }
        AiTool tool = tools.get(record.getToolName());
        try {
            String result = formatter.format(bridge.execute(tool, record.getArgumentsJson(), session));
            toolLogs.finish(record.getId(), "SUCCEEDED", result, Long.valueOf(session.getUserId()));
            conversations.saveAssistant(record.getSessionId(), record.getRequestId(), result, "COMPLETED");
            return new AiConfirmResult(record.getSessionId(), result);
        } catch (AiServiceException ex) {
            toolLogs.finish(record.getId(), "FAILED", ex.getMessage(), Long.valueOf(session.getUserId()));
            throw ex;
        }
    }

    public AiMonitorSnapshot monitor() {
        return toolLogs.monitor(model.isConfigured(), model.getModelName(), knowledge.activeCount());
    }

    private void handleTool(Message envelope, AiQuery query, String sessionId,
            SessionContext session, StreamWriter writer, AiToolInvocation invocation) {
        AiTool tool = tools.get(invocation.getToolName());
        if (tool == null) throw new AiServiceException(ResultCodes.NOT_FOUND, "未找到对应校园工具");
        long logId = toolLogs.create(sessionId, query.getRequestId(), tool.getName(),
                tool.isWriteOperation(), invocation.getArgumentsJson(), session.getUserId());
        if (tool.isWriteOperation()) {
            long expires = System.currentTimeMillis() + AiToolService.CONFIRM_TTL_MILLIS;
            writer.write(Message.action(envelope, new AiPendingAction(logId, query.getRequestId(),
                    sessionId, tool.getName(), invocation.getSummary(), expires)));
            String text = "该操作会修改业务数据，已等待你的确认：" + invocation.getSummary();
            conversations.saveAssistant(sessionId, query.getRequestId(), text, "COMPLETED");
            complete(envelope, query, sessionId, writer); return;
        }
        try {
            String result = formatter.format(bridge.execute(tool, invocation.getArgumentsJson(), session));
            toolLogs.finish(logId, "SUCCEEDED", result, null);
            conversations.saveAssistant(sessionId, query.getRequestId(), result, "COMPLETED");
            streamText(envelope, query, sessionId, writer, result); complete(envelope, query, sessionId, writer);
        } catch (AiServiceException ex) {
            toolLogs.finish(logId, "FAILED", ex.getMessage(), null); throw ex;
        }
    }

    private void answer(final Message envelope, final AiQuery query,
                        final String sessionId, final StreamWriter writer) throws Exception {
        final List<AiKnowledgeChunk> chunks = knowledge.retrieve(query.getText(), 5);
        final StringBuilder answer = new StringBuilder();
        if (model.isConfigured()) {
            try {
                model.generate(query.getRequestId(), prompts.build(query.getText(), chunks),
                        new AiTextSink() {
                            public void onText(String text) {
                                answer.append(text); writer.write(Message.stream(envelope,
                                        new AiStreamChunk(query.getRequestId(), sessionId, text, false)));
                            }
                        });
            } catch (InterruptedException ex) { throw ex; }
            catch (Exception ex) {
                String fallback = prompts.fallback(query.getText(), chunks);
                answer.setLength(0); answer.append(fallback);
                streamText(envelope, query, sessionId, writer, fallback);
            }
        } else {
            String fallback = prompts.fallback(query.getText(), chunks);
            answer.append(fallback); streamText(envelope, query, sessionId, writer, fallback);
        }
        conversations.saveAssistant(sessionId, query.getRequestId(), answer.toString(), "COMPLETED");
        complete(envelope, query, sessionId, writer);
    }

    private void streamText(Message envelope, AiQuery query, String sessionId,
                            StreamWriter writer, String text) {
        for (int start = 0; start < text.length(); start += 120) {
            int end = Math.min(text.length(), start + 120);
            writer.write(Message.stream(envelope, new AiStreamChunk(query.getRequestId(),
                    sessionId, text.substring(start, end), false)));
        }
    }

    private void complete(Message envelope, AiQuery query, String sessionId, StreamWriter writer) {
        writer.write(Message.stream(envelope,
                new AiStreamChunk(query.getRequestId(), sessionId, "", true)));
    }

    private void validate(AiQuery query) {
        if (query == null || blank(query.getRequestId()) || blank(query.getText())
                || query.getText().trim().length() > 4000) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "问题不能为空且不能超过 4000 字");
        }
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    private static final class ActiveQuery {
        private final Thread thread;
        private final long userId;
        private ActiveQuery(Thread thread, long userId) {
            this.thread = thread; this.userId = userId;
        }
    }
}
