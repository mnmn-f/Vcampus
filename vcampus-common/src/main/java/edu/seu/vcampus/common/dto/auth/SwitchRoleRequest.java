package edu.seu.vcampus.common.dto.auth;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;

/** 多职责用户切换当前工作台的请求。 */
public final class SwitchRoleRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Role role;

    public SwitchRoleRequest(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }
}
