package edu.seu.vcampus.server.security;

import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.common.security.RolePolicy;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** 一次登录会话的服务端可信身份上下文。 */
public final class SessionContext {
    private final String sessionToken;
    private final long userId;
    private final String account;
    private final String displayName;
    private final Set<Role> roles;
    private final Role activeRole;
    private final long createdAt;
    private volatile long lastAccessAt;

    public SessionContext(String sessionToken, long userId, String account,
                          String displayName, Set<Role> roles, Role activeRole) {
        this(sessionToken, userId, account, displayName, roles, activeRole,
                System.currentTimeMillis(), 0L);
    }

    private SessionContext(String sessionToken, long userId, String account,
                           String displayName, Set<Role> roles, Role activeRole,
                           long createdAt, long lastAccessAt) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionToken is required");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (account == null || account.trim().isEmpty()) {
            throw new IllegalArgumentException("account is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("displayName is required");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("at least one role is required");
        }
        if (activeRole == null || !roles.contains(activeRole)) {
            throw new IllegalArgumentException("activeRole must belong to roles");
        }
        this.sessionToken = sessionToken;
        this.userId = userId;
        this.account = account.trim();
        this.displayName = displayName.trim();
        this.roles = Collections.unmodifiableSet(EnumSet.copyOf(roles));
        this.activeRole = activeRole;
        this.createdAt = createdAt;
        this.lastAccessAt = lastAccessAt == 0L ? createdAt : lastAccessAt;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public long getUserId() {
        return userId;
    }

    public String getAccount() {
        return account;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Role getActiveRole() {
        return activeRole;
    }

    /** 兼容旧的单角色调用；服务端鉴权必须使用当前职责 {@link #allows(Permission)}。 */
    public Role getRole() {
        return activeRole;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastAccessAt() {
        return lastAccessAt;
    }

    public boolean hasRole(Role role) {
        return role != null && roles.contains(role);
    }

    public boolean allows(Permission permission) {
        return permission != null && RolePolicy.allows(activeRole, permission);
    }

    public SessionContext withActiveRole(Role role) {
        if (!hasRole(role)) {
            throw new IllegalArgumentException("role is not assigned to this session");
        }
        SessionContext replacement = new SessionContext(
                sessionToken, userId, account, displayName, roles, role,
                createdAt, lastAccessAt);
        return replacement;
    }

    void touch() {
        lastAccessAt = System.currentTimeMillis();
    }
}
