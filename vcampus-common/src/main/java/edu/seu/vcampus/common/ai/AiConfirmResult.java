package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 写操作确认或取消后的用户可读结果。 */
public final class AiConfirmResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sessionId;
    private final String content;

    public AiConfirmResult(String sessionId, String content) {
        this.sessionId = sessionId;
        this.content = content;
    }

    public String getSessionId() { return sessionId; }
    public String getContent() { return content; }
}
