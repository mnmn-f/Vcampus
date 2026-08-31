package edu.seu.vcampus.server.identity.repository.mysql;

import edu.seu.vcampus.common.dto.identity.MonitorSnapshotDto;
import edu.seu.vcampus.server.identity.repository.IdentityMonitorRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.threeten.bp.LocalDateTime;

/** users/user_sessions 的最小健康与运行计数查询。 */
public final class MySqlIdentityMonitorRepository implements IdentityMonitorRepository {
    @Override public MonitorSnapshotDto snapshot(Connection connection) {
        try {
            health(connection);
            return new MonitorSnapshotDto(true, activeSessions(connection), userCount(connection),
                    LocalDateTime.now(), "数据库连接正常");
        } catch (SQLException ex) {
            return new MonitorSnapshotDto(false, 0, 0L, LocalDateTime.now(), "数据库不可用");
        }
    }

    private static void health(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1"); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("health check returned no row");
        }
    }

    private static long userCount(Connection c) throws SQLException {
        return count(c, "SELECT COUNT(*) FROM users");
    }

    private static int activeSessions(Connection c) throws SQLException {
        return (int) count(c, "SELECT COUNT(*) FROM user_sessions WHERE revoked_at IS NULL "
                + "AND expires_at > CURRENT_TIMESTAMP(3)");
    }

    private static long count(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
