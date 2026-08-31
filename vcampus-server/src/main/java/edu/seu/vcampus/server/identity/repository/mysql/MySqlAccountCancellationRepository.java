package edu.seu.vcampus.server.identity.repository.mysql;

import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.identity.repository.AccountCancellationRecord;
import edu.seu.vcampus.server.identity.repository.IdentityCancellationRepository;
import edu.seu.vcampus.server.identity.repository.IdentityRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** account_cancellation_requests 的 MySQL PreparedStatement DAO。 */
public final class MySqlAccountCancellationRepository implements IdentityCancellationRepository {
    private static final String COLUMNS = "r.id, r.user_id, u.username, u.display_name, r.reason, "
            + "r.status, r.reviewed_by, r.reviewed_at, r.review_remark, r.created_at, r.updated_at";
    private static final String FROM = " FROM account_cancellation_requests r JOIN users u ON u.id = r.user_id";

    @Override public AccountCancellationPage searchAccountCancellations(Connection c,
                                                                          AccountCancellationQuery query,
                                                                          Long userId) {
        AccountCancellationQuery q = query == null ? new AccountCancellationQuery() : query;
        String where = filters(q, userId);
        List<AccountCancellationDto> items = new ArrayList<AccountCancellationDto>();
        String sql = "SELECT " + COLUMNS + FROM + where
                + " ORDER BY r.created_at DESC, r.id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = bind(ps, q, userId, 1);
            ps.setInt(i++, q.getPageSize());
            ps.setInt(i, q.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(read(rs).toDto());
            }
            return new AccountCancellationPage(items, q.getPage(), q.getPageSize(),
                    count(c, q, userId, where));
        } catch (SQLException ex) { throw failure("查询账号注销申请失败", ex); }
    }

    @Override public AccountCancellationRecord findAccountCancellation(Connection c,
                                                                                    long requestId,
                                                                                    boolean forUpdate) {
        return find(c, " WHERE r.id = ?", Long.valueOf(requestId), forUpdate);
    }

    @Override public AccountCancellationRecord findPendingAccountCancellation(Connection c,
                                                                                         long userId,
                                                                                         boolean forUpdate) {
        return find(c, " WHERE r.user_id = ? AND r.status = 'PENDING'", Long.valueOf(userId), forUpdate);
    }

    @Override public long insertAccountCancellation(Connection c, long userId, String reason) {
        String sql = "INSERT INTO account_cancellation_requests (user_id, reason, status) "
                + "VALUES (?, ?, 'PENDING')";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, reason);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new IdentityRepositoryException("注销申请编号生成失败");
                return keys.getLong(1);
            }
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                throw new IdentityRepositoryException(ResultCodes.CONFLICT, "已有待处理注销申请", ex);
            }
            throw failure("创建账号注销申请失败", ex);
        }
    }

    @Override public boolean withdrawAccountCancellation(Connection c, long requestId, long userId) {
        String sql = "UPDATE account_cancellation_requests SET status = 'CANCELLED', "
                + "reviewed_by = NULL, reviewed_at = NULL, review_remark = NULL "
                + "WHERE id = ? AND user_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw failure("撤回账号注销申请失败", ex); }
    }

    @Override public boolean reviewAccountCancellation(Connection c, long requestId, String status,
                                                       long reviewerId, String remark) {
        String sql = "UPDATE account_cancellation_requests SET status = ?, reviewed_by = ?, "
                + "reviewed_at = CURRENT_TIMESTAMP(3), review_remark = ? "
                + "WHERE id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, reviewerId);
            ps.setString(3, remark);
            ps.setLong(4, requestId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw failure("审批账号注销申请失败", ex); }
    }

    private AccountCancellationRecord find(Connection c, String where, Object value,
                                                      boolean forUpdate) {
        String sql = "SELECT " + COLUMNS + FROM + where + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        } catch (SQLException ex) { throw failure("读取账号注销申请失败", ex); }
    }

    private static AccountCancellationRecord read(ResultSet rs) throws SQLException {
        return new AccountCancellationRecord(rs.getLong(1), rs.getLong(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), nullableLong(rs, 7),
                time(rs.getTimestamp(8)), rs.getString(9), time(rs.getTimestamp(10)),
                time(rs.getTimestamp(11)));
    }

    private static String filters(AccountCancellationQuery q, Long userId) {
        StringBuilder result = new StringBuilder(" WHERE 1 = 1");
        if (userId != null) result.append(" AND r.user_id = ?");
        if (q.getStatus() != null) result.append(" AND r.status = ?");
        return result.toString();
    }

    private static int bind(PreparedStatement ps, AccountCancellationQuery q, Long userId, int index)
            throws SQLException {
        if (userId != null) ps.setLong(index++, userId.longValue());
        if (q.getStatus() != null) ps.setString(index++, q.getStatus());
        return index;
    }

    private static long count(Connection c, AccountCancellationQuery q, Long userId, String where)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*)" + FROM + where)) {
            bind(ps, q, userId, 1);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private static Long nullableLong(ResultSet rs, int index) throws SQLException {
        long value = rs.getLong(index);
        return rs.wasNull() ? null : Long.valueOf(value);
    }

    private static LocalDateTime time(Timestamp value) {
        return JdbcTemporal.localDateTime(value);
    }

    private static IdentityRepositoryException failure(String message, Throwable cause) {
        return new IdentityRepositoryException(message, cause);
    }
}
