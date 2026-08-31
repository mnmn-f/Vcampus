package edu.seu.vcampus.common.dto.identity;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;

/** 给用户分配角色请求。 */
public final class RoleAssignmentRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long userId;
    private final Role role;

    public RoleAssignmentRequest(long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public long getUserId() { return userId; }
    public long getId() { return userId; }
    public Role getRole() { return role; }
}
