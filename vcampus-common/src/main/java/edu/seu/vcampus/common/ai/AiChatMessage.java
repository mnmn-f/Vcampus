package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 可在客户端展示的历史对话消息。 */
public final class AiChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long messageId;
    private final String sessionId;
    private final String requestId;
    private final String senderType;
    private final String content;
    private final String status;
    private final long createdAt;

    public AiChatMessage(long messageId, String sessionId, String requestId,
                         String senderType, String content, String status,
                         long createdAt) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.senderType = senderType;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getMessageId() { return messageId; }
    public String getSessionId() { return sessionId; }
    public String getRequestId() { return requestId; }
    public String getSenderType() { return senderType; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
}
