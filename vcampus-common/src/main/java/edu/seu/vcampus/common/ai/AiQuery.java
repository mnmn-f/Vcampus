package edu.seu.vcampus.common.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 表示一次 AI 查询请求及其会话关联信息。 */
public final class AiQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String requestId;
    private final String sessionId;
    private final String text;
    private final AiMode mode;
    private final List<AiAttachment> attachments;

    public AiQuery(String requestId, String sessionId, String text) {
        this(requestId, sessionId, text, AiMode.QA);
    }

    public AiQuery(String requestId, String sessionId, String text, AiMode mode) {
        this(requestId, sessionId, text, mode, null);
    }

    public AiQuery(String requestId, String sessionId, String text, AiMode mode,
                   List<AiAttachment> attachments) {
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.text = text;
        this.mode = mode == null ? AiMode.QA : mode;
        this.attachments = attachments == null
                ? Collections.<AiAttachment>emptyList()
                : Collections.unmodifiableList(new ArrayList<AiAttachment>(attachments));
    }

    public String getRequestId() { return requestId; }
    public String getSessionId() { return sessionId; }
    public String getText() { return text; }
    public AiMode getMode() { return mode == null ? AiMode.QA : mode; }
    public List<AiAttachment> getAttachments() {
        return attachments == null ? Collections.<AiAttachment>emptyList() : attachments;
    }
}
