package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountTransactionDto;
import edu.seu.vcampus.server.store.repository.LedgerRecord;
import edu.seu.vcampus.server.store.repository.StoreAccountRepository;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** accounts 与 account_transactions 的 MySQL DAO。 */
public final class MySqlStoreAccountRepository implements StoreAccountRepository {
    @Override
    public AccountDto findAccount(Connection c, long userId, boolean forUpdate) {
        String sql = "SELECT id, user_id, balance, status FROM accounts WHERE user_id = ?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new AccountDto(rs.getLong("id"),
                        rs.getLong("user_id"), rs.getBigDecimal("balance"), rs.getString("status"))
                        : null;
            }
        } catch (SQLException ex) {
            throw fail("查询账户失败", ex);
        }
    }

    @Override
    public AccountLedgerPage findLedger(Connection c, long userId, AccountLedgerQuery query) {
        AccountLedgerQuery q = query == null ? new AccountLedgerQuery() : query;
        String type = q.getTransactionType();
        String where = type == null ? "" : " AND t.transaction_type = ?";
        String sql = "SELECT t.id, t.account_id, t.transaction_type, t.amount, "
                + "t.balance_before, t.balance_after, t.reference_type, t.reference_id, "
                + "t.idempotency_key, t.operator_id, t.remark, t.created_at "
                + "FROM account_transactions t JOIN accounts a ON a.id = t.account_id "
                + "WHERE a.user_id = ?" + where + " ORDER BY t.created_at DESC, t.id DESC LIMIT ? OFFSET ?";
        List<AccountTransactionDto> items = new ArrayList<AccountTransactionDto>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, userId);
            if (type != null) ps.setString(i++, type);
            ps.setInt(i++, q.getPageSize());
            ps.setInt(i, q.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(read(rs));
            }
            return new AccountLedgerPage(items, q.getPage(), q.getPageSize(),
                    count(c, userId, type));
        } catch (SQLException ex) {
            throw fail("查询账户流水失败", ex);
        }
    }

    @Override
    public LedgerRecord findTransactionByKey(Connection c, String key) {
        String sql = "SELECT account_id, amount, reference_type, reference_id, transaction_type "
                + "FROM account_transactions WHERE idempotency_key = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new LedgerRecord(rs.getLong("account_id"),
                        rs.getBigDecimal("amount"), rs.getString("reference_type"),
                        nullableLong(rs, "reference_id"), rs.getString("transaction_type"))
                        : null;
            }
        } catch (SQLException ex) {
            throw fail("查询幂等流水失败", ex);
        }
    }

    @Override
    public boolean updateAccountBalance(Connection c, long accountId, BigDecimal expected,
                                       BigDecimal updated) {
        String sql = "UPDATE accounts SET balance = ?, version = version + 1 "
                + "WHERE id = ? AND balance = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, updated);
            ps.setLong(2, accountId);
            ps.setBigDecimal(3, expected);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw fail("更新账户余额失败", ex);
        }
    }

    @Override
    public long insertTransaction(Connection c, long accountId, String type, BigDecimal amount,
                                  BigDecimal before, BigDecimal after, String referenceType,
                                  Long referenceId, String key, long operatorId, String remark) {
        String sql = "INSERT INTO account_transactions (account_id, transaction_type, amount, "
                + "balance_before, balance_after, reference_type, reference_id, idempotency_key, "
                + "operator_id, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, accountId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setBigDecimal(4, before);
            ps.setBigDecimal(5, after);
            setNullable(ps, 6, referenceType, java.sql.Types.VARCHAR);
            setNullable(ps, 7, referenceId, java.sql.Types.BIGINT);
            ps.setString(8, key);
            ps.setLong(9, operatorId);
            setNullable(ps, 10, remark, java.sql.Types.VARCHAR);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new StoreRepositoryException("流水编号生成失败");
                return rs.getLong(1);
            }
        } catch (SQLException ex) {
            throw fail("写入账户流水失败", ex);
        }
    }

    private long count(Connection c, long userId, String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM account_transactions t JOIN accounts a "
                + "ON a.id = t.account_id WHERE a.user_id = ?"
                + (type == null ? "" : " AND t.transaction_type = ?");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            if (type != null) ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private static AccountTransactionDto read(ResultSet rs) throws SQLException {
        return new AccountTransactionDto(rs.getLong("id"), rs.getLong("account_id"),
                rs.getString("transaction_type"), rs.getBigDecimal("amount"),
                rs.getBigDecimal("balance_before"), rs.getBigDecimal("balance_after"),
                rs.getString("reference_type"), nullableLong(rs, "reference_id"),
                rs.getString("idempotency_key"), nullableLong(rs, "operator_id"),
                rs.getString("remark"), MySqlStoreProductRepository.time(rs.getTimestamp("created_at")));
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }

    private static void setNullable(PreparedStatement ps, int index, Object value, int type)
            throws SQLException {
        if (value == null) ps.setNull(index, type);
        else if (value instanceof Long) ps.setLong(index, ((Long) value).longValue());
        else ps.setString(index, String.valueOf(value));
    }

    private static StoreRepositoryException fail(String message, Throwable cause) {
        return new StoreRepositoryException(message, cause);
    }
}
