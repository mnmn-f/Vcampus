package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.security.Role;

import java.sql.Connection;
import java.util.List;

/** roles 表的读取边界。 */
public interface IdentityRoleRepository {
    List<RoleDto> listRoles(Connection connection);
    IdentityRoleRecord findRole(Connection connection, Role role, boolean forUpdate);
}
