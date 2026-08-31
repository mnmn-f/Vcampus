package edu.seu.vcampus.server.identity.repository.mysql;

import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.UserPage;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.identity.repository.IdentityRepositoryException;
import edu.seu.vcampus.server.identity.repository.IdentityUserRecord;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** users/user_roles 的 MySQL 查询职责；写入由 MySqlIdentityUserRepository 负责。 */
public final class MySqlIdentityUserQueryRepository {
    private static final String COLUMNS = "u.id, u.username, u.password_hash, u.display_name, "
            + "u.email, u.phone, u.avatar_url, u.status, u.created_at, u.updated_at, u.last_login_at";
    private static final String FROM = " FROM users u";

    public IdentityUserRecord findById(Connection c, long id, boolean lock) {
        return find(c, " WHERE u.id = ?", Long.valueOf(id), lock);
    }

    public IdentityUserRecord findByAccount(Connection c, String account, boolean lock) {
        return find(c, " WHERE u.username = ?", account, lock);
    }

    public UserPage search(Connection c, UserQuery query) {
        UserQuery q = query == null ? new UserQuery() : query;
        String where = filters(q);
        List<ProfileDto> items = new ArrayList<ProfileDto>();
        String sql = "SELECT " + COLUMNS + FROM + where + " ORDER BY u.id LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = bindFilters(ps, q, 1);
            ps.setInt(i++, q.getPageSize());
            ps.setInt(i, q.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(read(rs, c).toProfile());
            }
            return new UserPage(items, q.getPage(), q.getPageSize(), count(c, q, where));
        } catch (SQLException ex) { throw failure("查询用户失败", ex); }
    }

    private IdentityUserRecord find(Connection c, String where, Object value,
                                               boolean lock) {
        String sql = "SELECT " + COLUMNS + FROM + where + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs, c) : null;
            }
        } catch (SQLException ex) { throw failure("读取用户失败", ex); }
    }

    private IdentityUserRecord read(ResultSet rs, Connection c) throws SQLException {
        long id = rs.getLong("id");
        return new IdentityUserRecord(id, rs.getString("username"), rs.getString("password_hash"),
                rs.getString("display_name"), rs.getString("email"), rs.getString("phone"),
                rs.getString("avatar_url"), rs.getString("status"), roles(c, id),
                time(rs.getTimestamp("created_at")), time(rs.getTimestamp("updated_at")),
                time(rs.getTimestamp("last_login_at")));
    }

    private Set<Role> roles(Connection c, long id) throws SQLException {
        EnumSet<Role> result = EnumSet.noneOf(Role.class);
        try (PreparedStatement ps = c.prepareStatement("SELECT r.code FROM user_roles ur "
                + "JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.status = 'ACTIVE'")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try { result.add(Role.valueOf(rs.getString(1))); }
                    catch (IllegalArgumentException ex) { throw new SQLException("unknown role", ex); }
                }
            }
        }
        return result;
    }

    private long count(Connection c, UserQuery q, String where) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*)" + FROM + where)) {
            bindFilters(ps, q, 1);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private static String filters(UserQuery q) {
        StringBuilder result = new StringBuilder(" WHERE 1 = 1");
        if (q.getKeyword() != null) result.append(" AND (u.username LIKE ? OR u.display_name LIKE ?)");
        if (q.getStatus() != null) result.append(" AND u.status = ?");
        return result.toString();
    }

    private static int bindFilters(PreparedStatement ps, UserQuery q, int i) throws SQLException {
        if (q.getKeyword() != null) {
            String value = "%" + q.getKeyword() + "%";
            ps.setString(i++, value);
            ps.setString(i++, value);
        }
        if (q.getStatus() != null) ps.setString(i++, q.getStatus());
        return i;
    }

    private static LocalDateTime time(Timestamp value) {
        return JdbcTemporal.localDateTime(value);
    }

    private static IdentityRepositoryException failure(String message, Throwable cause) {
        return new IdentityRepositoryException(message, cause);
    }
}
