package edu.seu.vcampus.common.dto.identity;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** 脱敏用户资料；不包含 password_hash。 */
public final class ProfileDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long userId;
    private final String account;
    private final String displayName;
    private final String email;
    private final String phone;
    private final String avatarUrl;
    private final String status;
    private final Set<Role> roles;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime lastLoginAt;

    public ProfileDto(long userId, String account, String displayName, String email,
                      String phone, String avatarUrl, String status, Set<Role> roles,
                      LocalDateTime createdAt, LocalDateTime updatedAt,
                      LocalDateTime lastLoginAt) {
        this.userId = userId;
        this.account = account;
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

    public ProfileDto(long userId, String account, String displayName, String status,
                      Set<Role> roles) {
        this(userId, account, displayName, null, null, null, status, roles,
                null, null, null);
    }

    public long getUserId() { return userId; }
    public long getId() { return userId; }
    public String getAccount() { return account; }
    public String getUsername() { return account; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getStatus() { return status; }
    public Set<Role> getRoles() { return roles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
}
