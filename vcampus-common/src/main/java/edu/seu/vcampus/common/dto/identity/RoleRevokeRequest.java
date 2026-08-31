package edu.seu.vcampus.common.dto.identity;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;

/** 撤销用户角色请求。 */
public final class RoleRevokeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long userId;
    private final Role role;

    public RoleRevokeRequest(long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public long getUserId() { return userId; }
    public long getId() { return userId; }
    public Role getRole() { return role; }
}
