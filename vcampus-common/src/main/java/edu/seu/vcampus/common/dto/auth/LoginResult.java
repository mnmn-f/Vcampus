package edu.seu.vcampus.common.dto.auth;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** 封装登录成功后的用户身份、角色和会话令牌。 */
public final class LoginResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long userId;
    private final String account;
    private final String displayName;
    private final Set<Role> roles;
    private final Role activeRole;
    private final String sessionToken;

    public LoginResult(long userId, String account, String displayName,
                       Role role, String sessionToken) {
        this(userId, account, displayName, EnumSet.of(role), role, sessionToken);
    }

    public LoginResult(long userId, String account, String displayName,
                       Set<Role> roles, Role activeRole, String sessionToken) {
        this.userId = userId;
        this.account = account;
        this.displayName = displayName;
        this.roles = Collections.unmodifiableSet(EnumSet.copyOf(roles));
        this.activeRole = activeRole;
        this.sessionToken = sessionToken;
    }

    public long getUserId() { return userId; }
    public String getAccount() { return account; }
    public String getDisplayName() { return displayName; }
    public Set<Role> getRoles() { return roles; }
    public Role getActiveRole() { return activeRole; }
    /** 兼容只使用一个当前角色的界面代码。 */
    public Role getRole() { return activeRole; }
    public String getSessionToken() { return sessionToken; }
}
