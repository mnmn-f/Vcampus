package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.server.store.repository.StoreRepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** store_orders 物流字段的单一写入口。 */
final class MySqlStoreShippingWriter {
    boolean update(Connection connection, long orderId, String status,
                   String trackingNo, String remark) {
        String sql = "UPDATE store_orders SET shipping_status=?,tracking_no=?,shipping_remark=?,"
                + "status=CASE WHEN ?='DELIVERED' THEN 'COMPLETED' ELSE status END,"
                + "completed_at=CASE WHEN ?='DELIVERED' THEN COALESCE(completed_at,CURRENT_TIMESTAMP(3)) ELSE completed_at END,"
                + "version=version+1 WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status); statement.setString(2, trackingNo);
            statement.setString(3, remark); statement.setString(4, status);
            statement.setString(5, status); statement.setLong(6, orderId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new StoreRepositoryException("更新订单物流失败", ex);
        }
    }
}
