package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.security.Role;

import org.threeten.bp.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** 身份仓储内部用户记录；passwordHash 绝不直接进入 Common DTO。 */
public final class IdentityUserRecord {
    private final long userId;
    private final String account;
    private final String passwordHash;
    private final String displayName;
    private final String email;
    private final String phone;
    private final String avatarUrl;
    private final String status;
    private final Set<Role> roles;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime lastLoginAt;

    public IdentityUserRecord(long userId, String account, String passwordHash,
                              String displayName, String email, String phone,
                              String avatarUrl, String status, Set<Role> roles,
                              LocalDateTime createdAt, LocalDateTime updatedAt,
                              LocalDateTime lastLoginAt) {
        this.userId = userId;
        this.account = account;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.roles = roles == null || roles.isEmpty()
                ? Collections.<Role>emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(roles));
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public long getUserId() { return userId; }
    public String getAccount() { return account; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getStatus() { return status; }
    public Set<Role> getRoles() { return roles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }

    public ProfileDto toProfile() {
        return new ProfileDto(userId, account, displayName, email, phone, avatarUrl, status,
                roles, createdAt, updatedAt, lastLoginAt);
    }
}
