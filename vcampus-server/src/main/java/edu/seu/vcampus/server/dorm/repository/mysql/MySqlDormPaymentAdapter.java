package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormPaymentPort;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** 账户支付的 JDBC 适配器；仅依赖 accounts/account_transactions 表。 */
public final class MySqlDormPaymentAdapter implements DormPaymentPort {
    @Override
    public long charge(Connection c, long userId, BigDecimal amount, String key, long allocationId)
            throws SQLException {
        if (key == null || key.trim().isEmpty()) {
            throw new DormRepositoryException(DormCommands.INVALID_INPUT, "支付幂等键不能为空");
        }
        try (PreparedStatement s = c.prepareStatement("SELECT id FROM account_transactions WHERE idempotency_key=?")) {
            s.setString(1, key);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()) throw new DormRepositoryException(DormCommands.PAYMENT_DUPLICATE, "重复支付请求");
            }
        }
        long accountId;
        BigDecimal before;
        try (PreparedStatement s = c.prepareStatement("SELECT id,balance FROM accounts WHERE user_id=? AND status='ACTIVE' FOR UPDATE")) {
            s.setLong(1, userId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new DormRepositoryException(DormCommands.BILL_NOT_PAYABLE, "账户不可用");
                accountId = r.getLong(1); before = r.getBigDecimal(2);
            }
        }
        if (before.compareTo(amount) < 0) {
            throw new DormRepositoryException(DormCommands.BILL_NOT_PAYABLE, "账户余额不足");
        }
        BigDecimal after = before.subtract(amount);
        try (PreparedStatement s = c.prepareStatement("UPDATE accounts SET balance=?,version=version+1 WHERE id=? AND balance=?")) {
            s.setBigDecimal(1, after); s.setLong(2, accountId); s.setBigDecimal(3, before);
            if (s.executeUpdate() != 1) throw new DormRepositoryException(DormCommands.BILL_NOT_PAYABLE, "账户余额发生变化，请重试");
        }
        String sql = "INSERT INTO account_transactions(account_id,transaction_type,amount,balance_before,balance_after,"
                + "reference_type,reference_id,idempotency_key,operator_id,remark) VALUES(?,'DORM_BILL_PAYMENT',?,?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, accountId); s.setBigDecimal(2, amount.negate()); s.setBigDecimal(3, before);
            s.setBigDecimal(4, after); s.setString(5, "UTILITY_ALLOCATION"); s.setLong(6, allocationId);
            s.setString(7, key); s.setLong(8, userId); s.setString(9, "宿舍水电分摊"); s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) {
                if (!r.next()) throw new SQLException("payment transaction id was not generated");
                return r.getLong(1);
            }
        }
    }
}
