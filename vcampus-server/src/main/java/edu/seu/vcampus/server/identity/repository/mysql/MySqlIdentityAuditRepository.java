package edu.seu.vcampus.server.identity.repository.mysql;

import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.BusinessAuditDto;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditDto;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditWriteRequest;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.identity.repository.IdentityAuditRepository;
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

/** login_audits/audit_logs 的 MySQL PreparedStatement DAO。 */
public final class MySqlIdentityAuditRepository implements IdentityAuditRepository {
    @Override public void insertLoginAudit(Connection c, LoginAuditWriteRequest r) {
        String sql = "INSERT INTO login_audits (user_id, username_snapshot, role_id, result_code, client_ip) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            nullableLong(ps, 1, r.getUserId());
            ps.setString(2, r.getUsernameSnapshot());
            nullableLong(ps, 3, roleId(c, r.getRole()));
            ps.setString(4, r.getResultCode());
            nullableString(ps, 5, r.getClientIp());
            ps.executeUpdate();
        } catch (SQLException ex) { throw fail("写入登录审计失败", ex); }
    }

    @Override public void insertBusinessAudit(Connection c, SessionContext actor, String action,
                                              String type, Long id, String outcome, String detail) {
        String sql = "INSERT INTO audit_logs (actor_user_id, actor_role_id, action, resource_type, "
                + "resource_id, outcome, detail_json) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            nullableLong(ps, 1, actor == null ? null : Long.valueOf(actor.getUserId()));
            nullableLong(ps, 2, roleId(c, actor == null ? null : actor.getActiveRole()));
            ps.setString(3, action);
            nullableString(ps, 4, type);
            nullableLong(ps, 5, id);
            ps.setString(6, outcome);
            nullableString(ps, 7, detail);
            ps.executeUpdate();
        } catch (SQLException ex) { throw fail("写入业务审计失败", ex); }
    }

    @Override public LoginAuditPage searchLoginAudits(Connection c, AuditQuery query) {
        AuditQuery q = query == null ? new AuditQuery() : query;
        String where = " WHERE 1 = 1";
        List<Object> args = new ArrayList<Object>();
        if (q.getUserId() != null) { where += " AND user_id = ?"; args.add(q.getUserId()); }
        if (q.getOutcome() != null) { where += " AND result_code = ?"; args.add(q.getOutcome()); }
        String base = " FROM login_audits" + where;
        String sql = "SELECT id, user_id, username_snapshot, role_id, result_code, client_ip, occurred_at"
                + base + " ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = bind(ps, args, 1);
            ps.setInt(i++, q.getPageSize());
            ps.setInt(i, q.getOffset());
            List<LoginAuditDto> items = new ArrayList<LoginAuditDto>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(login(rs));
            }
            return new LoginAuditPage(items, q.getPage(), q.getPageSize(), count(c, base, args));
        } catch (SQLException ex) { throw fail("查询登录审计失败", ex); }
    }

    @Override public BusinessAuditPage searchBusinessAudits(Connection c, AuditQuery query) {
        AuditQuery q = query == null ? new AuditQuery() : query;
        String where = " WHERE 1 = 1";
        List<Object> args = new ArrayList<Object>();
        if (q.getUserId() != null) { where += " AND actor_user_id = ?"; args.add(q.getUserId()); }
        if (q.getAction() != null) { where += " AND action = ?"; args.add(q.getAction()); }
        if (q.getOutcome() != null) { where += " AND outcome = ?"; args.add(q.getOutcome()); }
        String base = " FROM audit_logs" + where;
        String sql = "SELECT id, actor_user_id, actor_role_id, action, resource_type, resource_id, "
                + "outcome, detail_json, occurred_at" + base
                + " ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = bind(ps, args, 1);
            ps.setInt(i++, q.getPageSize());
            ps.setInt(i, q.getOffset());
            List<BusinessAuditDto> items = new ArrayList<BusinessAuditDto>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(business(rs));
            }
            return new BusinessAuditPage(items, q.getPage(), q.getPageSize(), count(c, base, args));
        } catch (SQLException ex) { throw fail("查询业务审计失败", ex); }
    }

    private static LoginAuditDto login(ResultSet rs) throws SQLException {
        return new LoginAuditDto(rs.getLong(1), nullableLong(rs, 2), rs.getString(3),
                role(rs.getString(4)), rs.getString(5), rs.getString(6), time(rs.getTimestamp(7)));
    }

    private static BusinessAuditDto business(ResultSet rs) throws SQLException {
        return new BusinessAuditDto(rs.getLong(1), nullableLong(rs, 2), role(rs.getString(3)),
                rs.getString(4), rs.getString(5), nullableLong(rs, 6), rs.getString(7),
                rs.getString(8), time(rs.getTimestamp(9)));
    }

    private static long count(Connection c, String base, List<Object> args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*)" + base)) {
            bind(ps, args, 1);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private static int bind(PreparedStatement ps, List<Object> args, int i) throws SQLException {
        for (Object value : args) {
            if (value instanceof Long) ps.setLong(i++, ((Long) value).longValue());
            else ps.setString(i++, String.valueOf(value));
        }
        return i;
    }

    private static Long roleId(Connection c, Role role) throws SQLException {
        if (role == null) return null;
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM roles WHERE code = ?")) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Long.valueOf(rs.getLong(1)) : null; }
        }
    }

    private static Role role(String code) {
        if (code == null) return null;
        try { return Role.valueOf(code); } catch (IllegalArgumentException ex) { return null; }
    }

    private static LocalDateTime time(Timestamp value) {
        return JdbcTemporal.localDateTime(value);
    }

    private static void nullableLong(PreparedStatement ps, int i, Long value) throws SQLException {
        if (value == null) ps.setNull(i, java.sql.Types.BIGINT); else ps.setLong(i, value.longValue());
    }

    private static void nullableString(PreparedStatement ps, int i, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) ps.setNull(i, java.sql.Types.VARCHAR);
        else ps.setString(i, value.trim());
    }

    private static Long nullableLong(ResultSet rs, int i) throws SQLException {
        long value = rs.getLong(i);
        return rs.wasNull() ? null : Long.valueOf(value);
    }

    private static IdentityRepositoryException fail(String message, Throwable cause) {
        return new IdentityRepositoryException(message, cause);
    }
}
