package edu.seu.vcampus.client.session;

import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** 客户端当前会话；导航只读取这里的 activeRole，不信任页面传入角色。 */
public final class ClientSession {
    private LoginResult loginResult;
    private Role activeRole;

    public void open(LoginResult result) {
        if (result == null || result.getRoles() == null || result.getRoles().isEmpty()) {
            throw new IllegalArgumentException("登录结果或角色不能为空");
        }
        this.loginResult = result;
        this.activeRole = result.getActiveRole() == null
                ? result.getRoles().iterator().next() : result.getActiveRole();
        if (!result.getRoles().contains(activeRole)) {
            this.activeRole = result.getRoles().iterator().next();
        }
    }

    public void close() {
        loginResult = null;
        activeRole = null;
    }

    public boolean isAuthenticated() {
        return loginResult != null;
    }

    public LoginResult getLoginResult() {
        return loginResult;
    }

    public long getUserId() {
        requireLogin();
        return loginResult.getUserId();
    }

    public String getDisplayName() {
        requireLogin();
        return loginResult.getDisplayName();
    }

    public String getAccount() {
        requireLogin();
        return loginResult.getAccount();
    }

    public String getSessionToken() {
        requireLogin();
        return loginResult.getSessionToken();
    }

    public Role getActiveRole() {
        requireLogin();
        return activeRole;
    }

    public Set<Role> getRoles() {
        if (loginResult == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(loginResult.getRoles()));
    }

    public void switchRole(Role role) {
        requireLogin();
        if (role == null || !loginResult.getRoles().contains(role)) {
            throw new IllegalArgumentException("当前用户不拥有该角色");
        }
        activeRole = role;
    }

    private void requireLogin() {
        if (loginResult == null) {
            throw new IllegalStateException("当前没有登录会话");
        }
    }
}
