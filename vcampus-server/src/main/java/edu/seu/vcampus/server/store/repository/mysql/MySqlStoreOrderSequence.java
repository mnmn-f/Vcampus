package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.threeten.bp.LocalDate;

/** 在订单事务内分配并发安全的每日流水号。 */
final class MySqlStoreOrderSequence {
    int next(Connection connection, LocalDate date) {
        String sql = "INSERT INTO store_order_daily_sequences "
                + "(order_date,last_sequence) VALUES (?,LAST_INSERT_ID(1)) "
                + "ON DUPLICATE KEY UPDATE last_sequence=LAST_INSERT_ID(last_sequence+1)";
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setDate(1, java.sql.Date.valueOf(date.toString()));
            prepared.executeUpdate();
            return selected(connection);
        } catch (SQLException ex) {
            throw new StoreRepositoryException("生成订单流水号失败", ex);
        }
    }

    private static int selected(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT LAST_INSERT_ID()")) {
            if (!result.next()) throw new StoreRepositoryException("订单流水号生成失败");
            int value = result.getInt(1);
            if (value <= 0 || value > 9999) {
                throw new StoreRepositoryException("当天订单数量已达到上限");
            }
            return value;
        }
    }
}
