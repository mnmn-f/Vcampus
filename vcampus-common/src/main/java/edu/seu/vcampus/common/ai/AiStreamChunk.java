package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 一次回答的流式文本分片。 */
public final class AiStreamChunk implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String requestId;
    private final String sessionId;
    private final String text;
    private final boolean completed;

    public AiStreamChunk(String requestId, String sessionId,
                         String text, boolean completed) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.text = text == null ? "" : text;
        this.completed = completed;
    }

    public String getRequestId() { return requestId; }
    public String getSessionId() { return sessionId; }
    public String getText() { return text; }
    public boolean isCompleted() { return completed; }
}
