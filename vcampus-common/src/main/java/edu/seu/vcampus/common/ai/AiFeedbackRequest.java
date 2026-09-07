package edu.seu.vcampus.common.ai;

import java.io.Serializable;

public final class AiFeedbackRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sessionId;
    private final String requestId;
    private final String rating;
    private final String category;
    private final String comment;
    public AiFeedbackRequest(String sessionId, String requestId, String rating,
            String category, String comment) {
        this.sessionId = sessionId; this.requestId = requestId; this.rating = rating;
        this.category = category; this.comment = comment;
    }
    public String getSessionId() { return sessionId; }
    public String getRequestId() { return requestId; }
    public String getRating() { return rating; }
    public String getCategory() { return category; }
    public String getComment() { return comment; }
}
