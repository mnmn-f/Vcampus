package edu.seu.vcampus.common.ai;

import java.io.Serializable;

public final class AiSessionRenameRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sessionId;
    private final String title;
    public AiSessionRenameRequest(String sessionId, String title) {
        this.sessionId = sessionId; this.title = title;
    }
    public String getSessionId() { return sessionId; }
    public String getTitle() { return title; }
}
