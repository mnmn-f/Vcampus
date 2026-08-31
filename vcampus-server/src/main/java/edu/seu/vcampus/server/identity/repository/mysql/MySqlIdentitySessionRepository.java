package edu.seu.vcampus.server.identity.repository.mysql;

import edu.seu.vcampus.common.dto.identity.SessionDto;
import edu.seu.vcampus.common.dto.identity.SessionPage;
import edu.seu.vcampus.common.dto.identity.SessionQuery;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.identity.repository.IdentitySessionRepository;
import edu.seu.vcampus.server.identity.repository.IdentityRepositoryException;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** user_sessions 的 MySQL DAO；只返回脱敏会话摘要。 */
public final class MySqlIdentitySessionRepository implements IdentitySessionRepository {
    private static final String COLUMNS = "s.id, s.user_id, u.username, r.code, s.created_at, "
            + "s.last_seen_at, s.expires_at, s.revoked_at";
    private static final String FROM = " FROM user_sessions s JOIN users u ON u.id = s.user_id "
            + "LEFT JOIN roles r ON r.id = s.current_role_id";

    @Override public SessionPage search(Connection c, SessionQuery query) {
        SessionQuery q = query == null ? new SessionQuery() : query;
        String where = filters(q);
        List<SessionDto> items = new ArrayList<SessionDto>();
        String sql = "SELECT " + COLUMNS + FROM + where
                + " ORDER BY s.last_seen_at DESC, s.id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = bind(ps, q, 1);
            ps.setInt(i++, q.getPageSize());
            ps.setInt(i, q.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(read(rs));
            }
            return new SessionPage(items, q.getPage(), q.getPageSize(), count(c, q, where));
        } catch (SQLException ex) { throw fail("查询会话失败", ex); }
    }

    @Override public boolean revoke(Connection c, long id) {
        String sql = "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP(3) "
                + "WHERE id = ? AND revoked_at IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw fail("强制下线失败", ex); }
    }

    @Override public int revokeUserSessions(Connection c, long userId) {
        return revokePersistedUserSessions(c, userId);
    }

    @Override public int revokePersistedUserSessions(Connection c, long userId) {
        String sql = "UPDATE user_sessions SET revoked_at = CURRENT_TIMESTAMP(3) "
                + "WHERE user_id = ? AND revoked_at IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        } catch (SQLException ex) { throw fail("撤销用户会话失败", ex); }
    }

    @Override public void observe(SessionContext session) {
        // Existing AuthService owns SessionManager; persisted login session hooks are optional.
    }

    private static String filters(SessionQuery q) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q.getUserId() != null) where.append(" AND s.user_id = ?");
        if (!q.isIncludeRevoked()) {
            where.append(" AND s.revoked_at IS NULL AND s.expires_at > CURRENT_TIMESTAMP(3)");
        }
        return where.toString();
    }

    private static int bind(PreparedStatement ps, SessionQuery q, int i) throws SQLException {
        if (q.getUserId() != null) ps.setLong(i++, q.getUserId().longValue());
        return i;
    }

    private static long count(Connection c, SessionQuery q, String where) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*)" + FROM + where)) {
            bind(ps, q, 1);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private static SessionDto read(ResultSet rs) throws SQLException {
        String code = rs.getString(4);
        Role role = null;
        if (code != null) {
            try { role = Role.valueOf(code); } catch (IllegalArgumentException ignored) { }
        }
        return new SessionDto(rs.getLong(1), rs.getLong(2), rs.getString(3), role,
                time(rs.getTimestamp(5)), time(rs.getTimestamp(6)), time(rs.getTimestamp(7)),
                time(rs.getTimestamp(8)));
    }

    private static LocalDateTime time(Timestamp value) {
        return JdbcTemporal.localDateTime(value);
    }

    private static IdentityRepositoryException fail(String message, Throwable cause) {
        return new IdentityRepositoryException(message, cause);
    }
}
