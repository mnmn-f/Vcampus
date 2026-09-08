package edu.seu.vcampus.common.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一次回答的流式文本分片。 */
public final class AiStreamChunk implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String requestId;
    private final String sessionId;
    private final String text;
    private final boolean completed;
    private final List<AiAnswerEvidence> evidence;

    public AiStreamChunk(String requestId, String sessionId,
                         String text, boolean completed) {
        this(requestId, sessionId, text, completed, null);
    }

    public AiStreamChunk(String requestId, String sessionId,
                         String text, boolean completed, List<AiAnswerEvidence> evidence) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.text = text == null ? "" : text;
        this.completed = completed;
        this.evidence = Collections.unmodifiableList(new ArrayList<AiAnswerEvidence>(
                evidence == null ? Collections.<AiAnswerEvidence>emptyList() : evidence));
    }

    public String getRequestId() { return requestId; }
    public String getSessionId() { return sessionId; }
    public String getText() { return text; }
    public boolean isCompleted() { return completed; }
    public List<AiAnswerEvidence> getEvidence() { return evidence; }
}
