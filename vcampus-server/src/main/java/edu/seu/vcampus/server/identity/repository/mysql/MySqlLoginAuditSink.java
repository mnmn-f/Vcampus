package edu.seu.vcampus.server.identity.repository.mysql;

import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.auth.LoginAuditSink;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** AuthService 的 MySQL 登录审计适配器；只写认证结果，不接触密码。 */
public final class MySqlLoginAuditSink implements LoginAuditSink {
    private final JdbcConnectionFactory connections;

    public MySqlLoginAuditSink(JdbcConnectionFactory connections) {
        if (connections == null) throw new IllegalArgumentException("connections is required");
        this.connections = connections;
    }

    @Override public void record(Long userId, String account, Role role, String resultCode,
                                 String clientIp) {
        String sql = "INSERT INTO login_audits (user_id, username_snapshot, role_id, result_code, client_ip) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = connections.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            nullableLong(ps, 1, userId);
            ps.setString(2, account == null || account.trim().isEmpty() ? "?" : account.trim());
            nullableLong(ps, 3, roleId(c, role));
            ps.setString(4, resultCode == null ? "AUTH.UNKNOWN" : resultCode);
            if (clientIp == null || clientIp.trim().isEmpty()) ps.setNull(5, java.sql.Types.VARCHAR);
            else ps.setString(5, clientIp.trim());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("写入登录审计失败", ex);
        }
    }

    private static Long roleId(Connection c, Role role) throws SQLException {
        if (role == null) return null;
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM roles WHERE code = ?")) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Long.valueOf(rs.getLong(1)) : null;
            }
        }
    }

    private static void nullableLong(PreparedStatement ps, int index, Long value)
            throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.BIGINT);
        else ps.setLong(index, value.longValue());
    }
}
