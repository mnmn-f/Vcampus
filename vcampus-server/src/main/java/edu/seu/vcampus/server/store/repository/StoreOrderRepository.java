package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

/** store_orders/store_order_items 及商品库存的事务写边界。 */
public interface StoreOrderRepository {
    long insertOrder(Connection connection, long buyerId, String orderNo,
                     BigDecimal totalAmount);
    void updateOrderPricing(Connection connection, long orderId, BigDecimal originalAmount,
                            BigDecimal discountAmount, String promotionCode,
                            String couponCode, String paymentMode);
    void insertOrderItems(Connection connection, long orderId, List<CartLine> lines);
    OrderDto findOrder(Connection connection, long orderId, boolean forUpdate);
    OrderPage findOrders(Connection connection, Long buyerId, OrderQuery query);
    boolean decrementStock(Connection connection, long productId, int quantity);
    void incrementStock(Connection connection, long productId, int quantity);
    boolean updateOrderStatus(Connection connection, long orderId, String status);
    boolean updateOrderShipping(Connection connection, long orderId, String shippingStatus,
                                String trackingNo, String remark);
}
