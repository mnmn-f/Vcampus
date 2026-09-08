package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 强制下线请求。 */
public final class SessionRevokeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long sessionId;

    public SessionRevokeRequest(long sessionId) { this.sessionId = sessionId; }

    public long getSessionId() { return sessionId; }
    public long getId() { return sessionId; }
}
