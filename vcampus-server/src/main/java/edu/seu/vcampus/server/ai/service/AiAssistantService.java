package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.ai.AiActionConfirmation;
import edu.seu.vcampus.common.ai.AiAnswerEvidence;
import edu.seu.vcampus.common.ai.AiAttachment;
import edu.seu.vcampus.common.ai.AiConfirmResult;
import edu.seu.vcampus.common.ai.AiKnowledgeChunk;
import edu.seu.vcampus.common.ai.AiKnowledgeTestResult;
import edu.seu.vcampus.common.ai.AiMode;
import edu.seu.vcampus.common.ai.AiMonitorSnapshot;
import edu.seu.vcampus.common.ai.AiPendingAction;
import edu.seu.vcampus.common.ai.AiQuery;
import edu.seu.vcampus.common.ai.AiStreamChunk;
import edu.seu.vcampus.common.ai.AiToolStatus;
import edu.seu.vcampus.common.ai.AiToolRouteTestResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.model.AiModel;
import edu.seu.vcampus.server.ai.model.AiTextSink;
import edu.seu.vcampus.server.ai.repository.AiToolRecord;
import edu.seu.vcampus.server.ai.repository.AiToolMetric;
import edu.seu.vcampus.server.ai.tool.AiTool;
import edu.seu.vcampus.server.ai.tool.AiToolInvocation;
import edu.seu.vcampus.server.ai.tool.AiToolRegistry;
import edu.seu.vcampus.server.ai.tool.ToolBridge;
import edu.seu.vcampus.server.ai.tool.ToolIntentParser;
import edu.seu.vcampus.server.ai.tool.ToolResultFormatter;
import edu.seu.vcampus.server.ai.tool.ModelToolIntentResolver;
import edu.seu.vcampus.server.ai.tool.NamedEntityResolver;
import edu.seu.vcampus.server.router.StreamWriter;
import edu.seu.vcampus.server.security.SessionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final ModelToolIntentResolver modelIntents;
    private final NamedEntityResolver entities;
    private final AiQueryLimiter limiter = new AiQueryLimiter();

    public AiAssistantService(AiConversationService conversations, AiKnowledgeService knowledge,
            AiToolService toolLogs, AiToolRegistry tools, ToolBridge bridge, AiModel model) {
        this.conversations = conversations; this.knowledge = knowledge; this.toolLogs = toolLogs;
        this.tools = tools; this.bridge = bridge; this.model = model;
        this.modelIntents = new ModelToolIntentResolver(model, tools);
        this.entities = new NamedEntityResolver(tools, bridge);
    }

    public void query(final Message envelope, final AiQuery query,
                      final SessionContext session, final StreamWriter writer) {
        String requestId = query == null ? null : query.getRequestId();
        String sessionId = null;
        boolean registered = false;
        try {
            validate(query);
            limiter.register(requestId, session.getUserId());
            registered = true;
            sessionId = conversations.beginQuery(session, query.getSessionId(),
                    query.getRequestId(), query.getText());
            String history = conversations.recentContext(session, sessionId);
            if (query.getMode() == AiMode.CHAT) {
                answerChat(envelope, query, sessionId, writer, history);
                return;
            }
            AiToolInvocation invocation = intents.parse(query.getText());
            if (invocation == null) {
                invocation = modelIntents.resolve(query.getRequestId(), query.getText(),
                        history, query.getMode());
            } else {
                AiTool parsedTool = tools.get(invocation.getToolName());
                if (parsedTool != null && parsedTool.clarificationFor(
                        invocation.getArgumentsJson()) != null) {
                    AiToolInvocation enriched = modelIntents.resolve(query.getRequestId(),
                            query.getText(), history, query.getMode());
                    if (enriched != null && invocation.getToolName().equals(enriched.getToolName())) {
                        invocation = enriched;
                    }
                }
            }
            if (invocation != null) {
                AiTool selected = tools.get(invocation.getToolName());
                if (query.getMode() == AiMode.QA && selected != null
                        && selected.isWriteOperation()) {
                    String hint = "这是会修改数据的操作。请切换到“代办模式”后重试，系统仍会在执行前请你确认。";
                    conversations.saveAssistant(sessionId, query.getRequestId(), hint, "COMPLETED");
                    streamText(envelope, query, sessionId, writer, hint);
                    complete(envelope, query, sessionId, writer); return;
                }
                invocation = entities.resolve(invocation, session);
                String clarification = selected == null ? null
                        : selected.clarificationFor(invocation.getArgumentsJson());
                if (clarification != null) {
                    String text = "【还需要一点信息】\n" + clarification;
                    conversations.saveAssistant(sessionId, query.getRequestId(), text, "COMPLETED");
                    streamText(envelope, query, sessionId, writer, text);
                    complete(envelope, query, sessionId, writer); return;
                }
                handleTool(envelope, query, sessionId, session, writer, invocation);
            } else {
                answer(envelope, query, sessionId, writer, history);
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
        } finally {
            if (registered) limiter.release(requestId, session.getUserId());
        }
    }

    public boolean cancel(SessionContext session, String requestId) {
        return limiter.cancel(session.getUserId(), requestId);
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
            String result = formatToolResult(tool,
                    bridge.execute(tool, record.getArgumentsJson(), session));
            toolLogs.finish(record.getId(), "SUCCEEDED", result, Long.valueOf(session.getUserId()));
            conversations.saveAssistant(record.getSessionId(), record.getRequestId(), result, "COMPLETED");
            return new AiConfirmResult(record.getSessionId(), result);
        } catch (AiServiceException ex) {
            toolLogs.finish(record.getId(), "FAILED", ex.getMessage(), Long.valueOf(session.getUserId()));
            throw ex;
        } catch (RuntimeException ex) {
            toolLogs.finish(record.getId(), "FAILED", safeError(ex), Long.valueOf(session.getUserId()));
            throw new AiServiceException(ResultCodes.INTERNAL_ERROR, "校园工具执行失败", ex);
        }
    }

    public AiMonitorSnapshot monitor() {
        return toolLogs.monitor(model.isConfigured(), model.getModelName(), knowledge.activeCount());
    }

    /** 在不写入学生会话的情况下预览知识检索与最终答案。 */
    public AiKnowledgeTestResult testKnowledge(String question) {
        if (blank(question) || question.trim().length() > 4000) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "测试问题不能为空且不能超过 4000 字");
        }
        List<AiKnowledgeChunk> chunks = knowledge.retrieve(question.trim(), 5);
        if (!model.isConfigured()) {
            return new AiKnowledgeTestResult(prompts.fallback(question, chunks), chunks, false);
        }
        final StringBuilder answer = new StringBuilder();
        try {
            AiPlainTextFilter filter = new AiPlainTextFilter(new AiTextSink() {
                public void onText(String text) { answer.append(text); }
            });
            model.generate("knowledge-test-" + UUID.randomUUID().toString(),
                    prompts.build(question.trim(), chunks, ""), filter);
            filter.finish();
            return new AiKnowledgeTestResult(answer.toString(), chunks, true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiServiceException(ResultCodes.CONFLICT, "知识测试已取消");
        } catch (Exception ex) {
            return new AiKnowledgeTestResult(prompts.fallback(question, chunks), chunks, false);
        }
    }

    public List<AiToolStatus> toolStatuses() {
        List<AiToolStatus> result = new ArrayList<AiToolStatus>();
        Map<String, AiToolMetric> metrics = toolLogs.metrics();
        for (Map.Entry<String, AiTool> entry : tools.all().entrySet()) {
            AiTool tool = entry.getValue();
            AiToolMetric metric = metrics.get(tool.getName());
            result.add(new AiToolStatus(tool.getName(), tool.getDescription(),
                    tool.getParameterGuide(), tool.isWriteOperation(), bridge.isAvailable(tool),
                    metric == null ? 0 : metric.getCalls(),
                    metric == null ? 0 : metric.getSucceeded(),
                    metric == null ? 0 : metric.getFailed(),
                    metric == null ? 0 : metric.getAverageDurationMillis(),
                    metric == null ? 0 : metric.getLastCalledAt(),
                    metric == null ? null : metric.getLastError()));
        }
        return result;
    }

    /** 仅运行意图识别和参数完整性检查，不执行校园业务命令。 */
    public AiToolRouteTestResult testToolRoute(String question) {
        if (blank(question) || question.trim().length() > 4000) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "路由测试问题不能为空且不能超过 4000 字");
        }
        String clean = question.trim();
        AiToolInvocation invocation = intents.parse(clean);
        if (invocation == null) invocation = modelIntents.resolve(
                "route-test-" + UUID.randomUUID().toString(), clean, "", AiMode.TASK);
        if (invocation == null) return new AiToolRouteTestResult(false, null, null,
                null, "未识别到校园工具，将进入普通知识问答。", false);
        AiTool tool = tools.get(invocation.getToolName());
        if (tool == null) return new AiToolRouteTestResult(false, invocation.getToolName(), null,
                invocation.getArgumentsJson(), "路由命中了未注册工具。", false);
        return new AiToolRouteTestResult(true, tool.getName(), tool.getDescription(),
                invocation.getArgumentsJson(), tool.clarificationFor(invocation.getArgumentsJson()),
                tool.isWriteOperation());
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
            String result = formatToolResult(tool,
                    bridge.execute(tool, invocation.getArgumentsJson(), session));
            toolLogs.finish(logId, "SUCCEEDED", result, null);
            conversations.saveAssistant(sessionId, query.getRequestId(), result, "COMPLETED");
            streamText(envelope, query, sessionId, writer, result); complete(envelope, query, sessionId, writer);
        } catch (AiServiceException ex) {
            toolLogs.finish(logId, "FAILED", ex.getMessage(), null); throw ex;
        } catch (RuntimeException ex) {
            toolLogs.finish(logId, "FAILED", safeError(ex), null);
            throw new AiServiceException(ResultCodes.INTERNAL_ERROR, "校园工具执行失败", ex);
        }
    }

    private void answer(final Message envelope, final AiQuery query,
                        final String sessionId, final StreamWriter writer,
                        final String history) throws Exception {
        final List<AiKnowledgeChunk> chunks = knowledge.retrieve(query.getText(), 5);
        final StringBuilder answer = new StringBuilder();
        if (model.isConfigured()) {
            try {
                AiPlainTextFilter filter = new AiPlainTextFilter(new AiTextSink() {
                            public void onText(String text) {
                                answer.append(text); writer.write(Message.stream(envelope,
                                        new AiStreamChunk(query.getRequestId(), sessionId, text, false)));
                            }
                        });
                model.generate(query.getRequestId(), prompts.build(query.getText(), chunks, history),
                        filter);
                filter.finish();
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
        complete(envelope, query, sessionId, writer, evidence(chunks));
    }

    private void answerChat(final Message envelope, final AiQuery query,
            final String sessionId, final StreamWriter writer, final String history) throws Exception {
        final StringBuilder answer = new StringBuilder();
        if (!model.isConfigured()) {
            String fallback = "聊天模式需要配置大模型 API。校园实时查询仍可在问答模式使用，业务操作可在代办模式使用。";
            answer.append(fallback); streamText(envelope, query, sessionId, writer, fallback);
        } else {
            try {
                generateChat(envelope, query, sessionId, writer, history, answer);
            } catch (InterruptedException ex) { throw ex; }
            catch (Exception ex) {
                // 带附件的首次请求可能在模型端初始化视觉/文档通道时短暂失败。
                // 只在尚未输出任何字符时透明重试，避免重复已经展示的半段回答。
                if (query.getAttachments().isEmpty() || answer.length() > 0) throw ex;
                generateChat(envelope, query, sessionId, writer, history, answer);
            }
        }
        conversations.saveAssistant(sessionId, query.getRequestId(), answer.toString(), "COMPLETED");
        complete(envelope, query, sessionId, writer);
    }

    private void generateChat(final Message envelope, final AiQuery query,
            final String sessionId, final StreamWriter writer, final String history,
            final StringBuilder answer) throws Exception {
        AiPlainTextFilter filter = new AiPlainTextFilter(new AiTextSink() {
            public void onText(String text) {
                answer.append(text); writer.write(Message.stream(envelope,
                        new AiStreamChunk(query.getRequestId(), sessionId, text, false)));
            }
        });
        model.generate(query.getRequestId(), prompts.chat(query.getText(), history),
                query.getAttachments(), filter);
        filter.finish();
    }

    private String formatToolResult(AiTool tool, Object value) {
        return "【实时数据｜" + tool.getDescription() + "】\n" + formatter.format(value);
    }

    private String safeError(RuntimeException error) {
        String value = error.getMessage();
        if (value == null || value.trim().isEmpty()) value = error.getClass().getSimpleName();
        return value.length() > 500 ? value.substring(0, 500) : value;
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
        complete(envelope, query, sessionId, writer, null);
    }

    private void complete(Message envelope, AiQuery query, String sessionId, StreamWriter writer,
            List<AiAnswerEvidence> evidence) {
        writer.write(Message.stream(envelope,
                new AiStreamChunk(query.getRequestId(), sessionId, "", true, evidence)));
    }

    private List<AiAnswerEvidence> evidence(List<AiKnowledgeChunk> chunks) {
        List<AiAnswerEvidence> result = new ArrayList<AiAnswerEvidence>();
        for (AiKnowledgeChunk chunk : chunks) {
            String excerpt = chunk.getContent() == null ? "" : chunk.getContent().trim();
            if (excerpt.length() > 180) excerpt = excerpt.substring(0, 180) + "…";
            result.add(new AiAnswerEvidence(chunk.getChunkId(), chunk.getTitle(),
                    chunk.getSourceType(), excerpt, chunk.getUpdatedAt()));
        }
        return result;
    }

    private void validate(AiQuery query) {
        if (query == null || blank(query.getRequestId()) || blank(query.getText())
                || query.getText().trim().length() > 4000) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "问题不能为空且不能超过 4000 字");
        }
        List<AiAttachment> attachments = query.getAttachments();
        if (!attachments.isEmpty() && query.getMode() != AiMode.CHAT) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "附件仅可在聊天模式使用");
        }
        if (attachments.size() > 3) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "一次最多上传 3 个附件");
        }
        long total = 0L;
        for (AiAttachment attachment : attachments) {
            if (attachment == null || blank(attachment.getFileName())
                    || (!attachment.isImage() && !attachment.isText())) {
                throw new AiServiceException(ResultCodes.INVALID_INPUT,
                        "仅支持常见图片和文本/代码文件");
            }
            int size = attachment.getContent().length;
            if (size <= 0 || (attachment.isImage() && size > 2 * 1024 * 1024)
                    || (attachment.isText() && size > 256 * 1024)) {
                throw new AiServiceException(ResultCodes.INVALID_INPUT,
                        "图片不能超过 2 MB，文本文件不能超过 256 KB");
            }
            total += size;
        }
        if (total > 5 * 1024 * 1024L) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT, "附件总大小不能超过 5 MB");
        }
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }

}
