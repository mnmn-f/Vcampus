package edu.seu.vcampus.server.ai.repository;

/** 待确认工具调用的服务端内部快照。 */
public final class AiToolRecord {
    private final long id;
    private final String sessionId;
    private final String requestId;
    private final String toolName;
    private final String argumentsJson;
    private final String status;
    private final long requestedBy;
    private final long createdAt;

    public AiToolRecord(long id, String sessionId, String requestId, String toolName,
                        String argumentsJson, String status, long requestedBy, long createdAt) {
        this.id = id; this.sessionId = sessionId; this.requestId = requestId;
        this.toolName = toolName; this.argumentsJson = argumentsJson;
        this.status = status; this.requestedBy = requestedBy; this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getRequestId() { return requestId; }
    public String getToolName() { return toolName; }
    public String getArgumentsJson() { return argumentsJson; }
    public String getStatus() { return status; }
    public long getRequestedBy() { return requestedBy; }
    public long getCreatedAt() { return createdAt; }
}
