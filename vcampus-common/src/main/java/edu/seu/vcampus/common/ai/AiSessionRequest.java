package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 指定本人 AI 会话的请求。 */
public final class AiSessionRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sessionId;

    public AiSessionRequest(String sessionId) { this.sessionId = sessionId; }
    public String getSessionId() { return sessionId; }
}
