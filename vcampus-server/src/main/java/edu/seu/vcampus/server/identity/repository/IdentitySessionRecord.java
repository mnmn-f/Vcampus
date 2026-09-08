package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.SessionDto;
import edu.seu.vcampus.common.security.Role;

import org.threeten.bp.LocalDateTime;

/** 会话内部记录；rawToken 仅用于内存 SessionManager 撤销，永不出 DTO。 */
public final class IdentitySessionRecord {
    private final long id;
    private final long userId;
    private final String account;
    private final String rawToken;
    private final Role activeRole;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastSeenAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime revokedAt;

    public IdentitySessionRecord(long id, long userId, String account, String rawToken,
                                 Role activeRole, LocalDateTime createdAt,
                                 LocalDateTime lastSeenAt, LocalDateTime expiresAt,
                                 LocalDateTime revokedAt) {
        this.id = id;
        this.userId = userId;
        this.account = account;
        this.rawToken = rawToken;
        this.activeRole = activeRole;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getAccount() { return account; }
    public String getRawToken() { return rawToken; }
    public Role getActiveRole() { return activeRole; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }

    public SessionDto toDto() {
        return new SessionDto(id, userId, account, activeRole, createdAt, lastSeenAt,
                expiresAt, revokedAt);
    }
}
