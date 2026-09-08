package edu.seu.vcampus.server.identity.repository.mysql;

import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.identity.repository.IdentityRoleRecord;
import edu.seu.vcampus.server.identity.repository.IdentityRoleRepository;
import edu.seu.vcampus.server.identity.repository.IdentityRepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** roles 表的 MySQL PreparedStatement DAO。 */
public final class MySqlIdentityRoleRepository implements IdentityRoleRepository {
    @Override public List<RoleDto> listRoles(Connection c) {
        List<RoleDto> result = new ArrayList<RoleDto>();
        String sql = "SELECT id, code, display_name, description, status FROM roles "
                + "WHERE status = 'ACTIVE' ORDER BY code";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                IdentityRoleRecord role = read(rs);
                if (role.getRole() != null) result.add(role.toDto());
            }
            return result;
        } catch (SQLException ex) { throw fail("查询角色失败", ex); }
    }

    @Override public IdentityRoleRecord findRole(Connection c, Role role, boolean lock) {
        String sql = "SELECT id, code, display_name, description, status FROM roles WHERE code = ?"
                + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        } catch (SQLException ex) { throw fail("读取角色失败", ex); }
    }

    private static IdentityRoleRecord read(ResultSet rs) throws SQLException {
        Role role;
        try { role = Role.valueOf(rs.getString("code")); }
        catch (IllegalArgumentException ex) { role = null; }
        return new IdentityRoleRecord(rs.getLong("id"), role, rs.getString("display_name"),
                rs.getString("description"), rs.getString("status"));
    }

    private static IdentityRepositoryException fail(String message, Throwable cause) {
        return new IdentityRepositoryException(message, cause);
    }
}
