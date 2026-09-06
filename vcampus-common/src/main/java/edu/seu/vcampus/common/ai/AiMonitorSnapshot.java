package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 管理端可见的 AI 服务运行摘要，不包含密钥或原始 token。 */
public final class AiMonitorSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean modelConfigured;
    private final String modelName;
    private final long activeSessions;
    private final long messages;
    private final long knowledgeChunks;
    private final long pendingActions;
    private final long succeededTools;
    private final long failedTools;
    private final long messages24h;
    private final long toolCalls24h;
    private final long failedTools24h;
    private final long stalePendingActions;
    private final long feedbackCount;
    private final long negativeFeedbackCount;

    public AiMonitorSnapshot(boolean modelConfigured, String modelName,
            long activeSessions, long messages, long knowledgeChunks,
            long pendingActions, long succeededTools, long failedTools) {
        this(modelConfigured, modelName, activeSessions, messages, knowledgeChunks,
                pendingActions, succeededTools, failedTools, 0, 0, 0, 0, 0, 0);
    }

    public AiMonitorSnapshot(boolean modelConfigured, String modelName,
            long activeSessions, long messages, long knowledgeChunks,
            long pendingActions, long succeededTools, long failedTools,
            long messages24h, long toolCalls24h, long failedTools24h,
            long stalePendingActions, long feedbackCount, long negativeFeedbackCount) {
        this.modelConfigured = modelConfigured;
        this.modelName = modelName;
        this.activeSessions = activeSessions;
        this.messages = messages;
        this.knowledgeChunks = knowledgeChunks;
        this.pendingActions = pendingActions;
        this.succeededTools = succeededTools;
        this.failedTools = failedTools;
        this.messages24h = messages24h; this.toolCalls24h = toolCalls24h;
        this.failedTools24h = failedTools24h; this.stalePendingActions = stalePendingActions;
        this.feedbackCount = feedbackCount; this.negativeFeedbackCount = negativeFeedbackCount;
    }

    public boolean isModelConfigured() { return modelConfigured; }
    public String getModelName() { return modelName; }
    public long getActiveSessions() { return activeSessions; }
    public long getMessages() { return messages; }
    public long getKnowledgeChunks() { return knowledgeChunks; }
    public long getPendingActions() { return pendingActions; }
    public long getSucceededTools() { return succeededTools; }
    public long getFailedTools() { return failedTools; }
    public long getMessages24h() { return messages24h; }
    public long getToolCalls24h() { return toolCalls24h; }
    public long getFailedTools24h() { return failedTools24h; }
    public long getStalePendingActions() { return stalePendingActions; }
    public long getFeedbackCount() { return feedbackCount; }
    public long getNegativeFeedbackCount() { return negativeFeedbackCount; }
}
