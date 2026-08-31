package edu.seu.vcampus.common.dto.identity;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;

/** 可分配角色摘要。 */
public final class RoleDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final Role role;
    private final String displayName;
    private final String description;
    private final String status;

    public RoleDto(long id, Role role, String displayName, String description, String status) {
        this.id = id;
        this.role = role;
        this.displayName = displayName;
        this.description = description;
        this.status = status;
    }

    public RoleDto(Role role) {
        this(0L, role, role == null ? null : role.getDisplayName(), null, "ACTIVE");
    }

    public long getId() { return id; }
    public Role getRole() { return role; }
    public String getCode() { return role == null ? null : role.name(); }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
}
