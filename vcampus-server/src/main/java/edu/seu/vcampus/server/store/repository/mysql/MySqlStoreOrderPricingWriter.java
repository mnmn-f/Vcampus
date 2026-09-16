package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/** 保存订单价格快照，并兼容尚未升级促销字段的旧数据库。 */
final class MySqlStoreOrderPricingWriter {
    private static final String SQL = "UPDATE store_orders SET original_amount=?,"
            + "discount_amount=?,promotion_code=?,coupon_code=?,payment_mode=? WHERE id=?";

    void update(Connection connection, long id, BigDecimal original, BigDecimal discount,
                String promotion, String coupon, String mode) {
        try {
            execute(connection, id, original, discount, promotion, coupon, mode);
        } catch (SQLException first) {
            if (!tooLong(first) || promotion == null || promotion.length() <= 64) {
                throw new StoreRepositoryException("保存订单价格快照失败", first);
            }
            try {
                execute(connection, id, original, discount, legacySnapshot(promotion), coupon, mode);
            } catch (SQLException second) {
                second.addSuppressed(first);
                throw new StoreRepositoryException("保存订单价格快照失败", second);
            }
        }
    }

    private static void execute(Connection connection, long id, BigDecimal original,
                                BigDecimal discount, String promotion, String coupon,
                                String mode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setBigDecimal(1, original); statement.setBigDecimal(2, discount);
            text(statement, 3, promotion); text(statement, 4, coupon);
            statement.setString(5, mode == null ? "SELF" : mode);
            statement.setLong(6, id); statement.executeUpdate();
        }
    }

    static String legacySnapshot(String value) {
        if (value == null || value.length() <= 64) return value;
        int count = 1;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == ',') count++;
        }
        return "MULTI-" + count + "-" + Integer.toHexString(value.hashCode()).toUpperCase();
    }

    private static void text(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.VARCHAR);
        else statement.setString(index, value);
    }

    private static boolean tooLong(SQLException error) {
        return error.getErrorCode() == 1406 || "22001".equals(error.getSQLState());
    }
}
