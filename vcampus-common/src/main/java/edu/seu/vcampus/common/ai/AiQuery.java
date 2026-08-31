package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 表示一次 AI 查询请求及其会话关联信息。 */
public final class AiQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String requestId;
    private final String sessionId;
    private final String text;

    public AiQuery(String requestId, String sessionId, String text) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.text = text;
    }

    public String getRequestId() { return requestId; }
    public String getSessionId() { return sessionId; }
    public String getText() { return text; }
}
