package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 会话列表中的只读摘要。 */
public final class AiSessionSummary implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sessionId;
    private final String title;
    private final String modelName;
    private final long createdAt;
    private final long updatedAt;

    public AiSessionSummary(String sessionId, String title, String modelName,
                            long createdAt, long updatedAt) {
        this.sessionId = sessionId;
        this.title = title;
        this.modelName = modelName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getSessionId() { return sessionId; }
    public String getTitle() { return title; }
    public String getModelName() { return modelName; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    @Override public String toString() {
        return title == null || title.trim().isEmpty() ? "新会话" : title;
    }
}
