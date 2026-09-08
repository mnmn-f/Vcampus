package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 服务端推送给用户确认的业务写操作。 */
public final class AiPendingAction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long actionId;
    private final String requestId;
    private final String sessionId;
    private final String toolName;
    private final String summary;
    private final long expireAt;

    public AiPendingAction(long actionId, String requestId, String sessionId,
                           String toolName, String summary, long expireAt) {
        this.actionId = actionId;
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.toolName = toolName;
        this.summary = summary;
        this.expireAt = expireAt;
    }

    public long getActionId() { return actionId; }
    public String getRequestId() { return requestId; }
    public String getSessionId() { return sessionId; }
    public String getToolName() { return toolName; }
    public String getSummary() { return summary; }
    public long getExpireAt() { return expireAt; }
}
