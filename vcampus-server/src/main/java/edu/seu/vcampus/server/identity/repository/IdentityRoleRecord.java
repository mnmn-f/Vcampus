package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.security.Role;

/** 角色仓储内部记录。 */
public final class IdentityRoleRecord {
    private final long id;
    private final Role role;
    private final String displayName;
    private final String description;
    private final String status;

    public IdentityRoleRecord(long id, Role role, String displayName,
                              String description, String status) {
        this.id = id;
        this.role = role;
        this.displayName = displayName;
        this.description = description;
        this.status = status;
    }

    public long getId() { return id; }
    public Role getRole() { return role; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }

    public RoleDto toDto() {
        return new RoleDto(id, role, displayName, description, status);
    }
}
