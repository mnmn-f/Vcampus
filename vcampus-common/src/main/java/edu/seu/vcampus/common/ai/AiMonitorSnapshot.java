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

    public AiMonitorSnapshot(boolean modelConfigured, String modelName,
            long activeSessions, long messages, long knowledgeChunks,
            long pendingActions, long succeededTools, long failedTools) {
        this.modelConfigured = modelConfigured;
        this.modelName = modelName;
        this.activeSessions = activeSessions;
        this.messages = messages;
        this.knowledgeChunks = knowledgeChunks;
        this.pendingActions = pendingActions;
        this.succeededTools = succeededTools;
        this.failedTools = failedTools;
    }

    public boolean isModelConfigured() { return modelConfigured; }
    public String getModelName() { return modelName; }
    public long getActiveSessions() { return activeSessions; }
    public long getMessages() { return messages; }
    public long getKnowledgeChunks() { return knowledgeChunks; }
    public long getPendingActions() { return pendingActions; }
    public long getSucceededTools() { return succeededTools; }
    public long getFailedTools() { return failedTools; }
}
