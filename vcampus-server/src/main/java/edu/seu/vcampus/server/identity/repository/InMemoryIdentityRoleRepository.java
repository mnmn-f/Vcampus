package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.security.Role;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 角色内存仓储；初始角色来自公共 Role 枚举。 */
final class InMemoryIdentityRoleRepository implements IdentityRoleRepository {
    private final InMemoryIdentityState state;

    InMemoryIdentityRoleRepository(InMemoryIdentityState state) { this.state = state; }

    @Override public List<RoleDto> listRoles(Connection c) {
        synchronized (state) {
            List<RoleDto> result = new ArrayList<RoleDto>();
            for (IdentityRoleRecord role : state.roles.values()) {
                if ("ACTIVE".equalsIgnoreCase(role.getStatus())) result.add(role.toDto());
            }
            Collections.sort(result, new Comparator<RoleDto>() {
                @Override public int compare(RoleDto a, RoleDto b) {
                    return a.getCode().compareTo(b.getCode());
                }
            });
            return result;
        }
    }

    @Override public IdentityRoleRecord findRole(Connection c, Role role, boolean lock) {
        synchronized (state) { return state.roles.get(role); }
    }
}
