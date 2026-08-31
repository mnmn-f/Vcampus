package edu.seu.vcampus.common.dto.identity;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 会话脱敏摘要；不返回原始 session token。 */
public final class SessionDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long userId;
    private final String account;
    private final Role activeRole;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastSeenAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime revokedAt;

    public SessionDto(long id, long userId, String account, Role activeRole,
                      LocalDateTime createdAt, LocalDateTime lastSeenAt,
                      LocalDateTime expiresAt, LocalDateTime revokedAt) {
        this.id = id;
        this.userId = userId;
        this.account = account;
        this.activeRole = activeRole;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public long getId() { return id; }
    public long getSessionId() { return id; }
    public long getUserId() { return userId; }
    public String getAccount() { return account; }
    public Role getActiveRole() { return activeRole; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public boolean isRevoked() { return revokedAt != null; }
}
