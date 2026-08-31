package edu.seu.vcampus.server.repository;

import edu.seu.vcampus.common.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 服务端使用的用户认证记录。
 *
 * <p>密码字段只保存经过哈希的值；该对象不会作为客户端 DTO 直接发送。</p>
 */
public final class UserRecord {
    private final long userId;
    private final String account;
    private final String passwordHash;
    private final String displayName;
    private final Set<Role> roles;
    private final boolean enabled;

    public UserRecord(long userId, String account, String passwordHash,
                      String displayName, Role role, boolean enabled) {
        this(userId, account, passwordHash, displayName,
                EnumSet.of(requireRole(role)), enabled);
    }

    public UserRecord(long userId, String account, String passwordHash,
                      String displayName, Set<Role> roles, boolean enabled) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.account = requireText(account, "account");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.displayName = requireText(displayName, "displayName");
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("at least one role is required");
        }
        this.userId = userId;
        this.roles = Collections.unmodifiableSet(EnumSet.copyOf(roles));
        this.enabled = enabled;
    }

    public long getUserId() {
        return userId;
    }

    public String getAccount() {
        return account;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    /** 兼容单角色页面；多角色用户应使用 {@link #getRoles()}。 */
    public Role getRole() {
        return roles.iterator().next();
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static Role requireRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        return role;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
