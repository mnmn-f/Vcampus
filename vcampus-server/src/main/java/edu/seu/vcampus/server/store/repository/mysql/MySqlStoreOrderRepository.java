package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.server.store.repository.CartLine;
import edu.seu.vcampus.server.store.repository.StoreOrderRepository;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** 订单、明细和库存的 MySQL DAO。 */
public final class MySqlStoreOrderRepository implements StoreOrderRepository {
    private final MySqlStoreShippingWriter shipping = new MySqlStoreShippingWriter();
    @Override
    public long insertOrder(Connection c, long buyerId, String orderNo, BigDecimal total) {
        String sql = "INSERT INTO store_orders (order_no, buyer_id, total_amount, original_amount, "
                + "discount_amount, payment_mode, status) VALUES (?, ?, ?, ?, 0, 'SELF', 'CREATED')";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, orderNo);
            ps.setLong(2, buyerId);
            ps.setBigDecimal(3, total);
            ps.setBigDecimal(4, total);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new StoreRepositoryException("订单编号生成失败");
                return rs.getLong(1);
            }
        } catch (SQLException ex) {
            throw fail("创建订单失败", ex);
        }
    }

    @Override public void updateOrderPricing(Connection c, long id, BigDecimal original,
            BigDecimal discount, String promotion, String coupon, String mode) {
        String sql = "UPDATE store_orders SET original_amount=?,discount_amount=?,promotion_code=?,coupon_code=?,payment_mode=? WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, original); ps.setBigDecimal(2, discount);
            if (promotion == null) ps.setNull(3, java.sql.Types.VARCHAR); else ps.setString(3, promotion);
            if (coupon == null) ps.setNull(4, java.sql.Types.VARCHAR); else ps.setString(4, coupon);
            ps.setString(5, mode == null ? "SELF" : mode); ps.setLong(6, id); ps.executeUpdate();
        } catch (SQLException ex) { throw fail("保存订单价格快照失败", ex); }
    }

    @Override
    public void insertOrderItems(Connection c, long orderId, List<CartLine> lines) {
        String sql = "INSERT INTO store_order_items (order_id, product_id, "
                + "product_name_snapshot, unit_price_snapshot, quantity, line_amount) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (CartLine line : lines) {
                ps.setLong(1, orderId);
                ps.setLong(2, line.getProductId());
                ps.setString(3, line.getProductName());
                ps.setBigDecimal(4, line.getUnitPrice());
                ps.setInt(5, line.getQuantity());
                ps.setBigDecimal(6, line.lineAmount());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            throw fail("保存订单明细失败", ex);
        }
    }

    @Override
    public OrderDto findOrder(Connection c, long id, boolean forUpdate) {
        String sql = "SELECT id, order_no, buyer_id, total_amount, original_amount, discount_amount, promotion_code, coupon_code, payment_mode, status, shipping_status, tracking_no, shipping_remark, created_at, "
                + "paid_at, cancelled_at, completed_at FROM store_orders WHERE id = ?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readOrder(c, rs) : null;
            }
        } catch (SQLException ex) {
            throw fail("查询订单详情失败", ex);
        }
    }

    @Override
    public OrderPage findOrders(Connection c, Long buyerId, OrderQuery query) {
        OrderQuery q = query == null ? new OrderQuery() : query;
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<Object>();
        if (buyerId != null) { where.append(" AND buyer_id = ?"); args.add(buyerId); }
        if (q.getOrderNo() != null) { where.append(" AND order_no LIKE ?"); args.add("%" + q.getOrderNo() + "%"); }
        if (q.getStatus() != null) { where.append(" AND status = ?"); args.add(q.getStatus()); }
        String base = " FROM store_orders" + where;
        List<OrderDto> items = new ArrayList<OrderDto>();
        String sql = "SELECT id, order_no, buyer_id, total_amount, original_amount, discount_amount, promotion_code, coupon_code, payment_mode, status, shipping_status, tracking_no, shipping_remark, created_at, paid_at, "
                + "cancelled_at, completed_at" + base + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = bind(ps, args, 1);
            ps.setInt(i++, q.getPageSize());
            ps.setInt(i, q.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(readOrder(c, rs));
            }
            long total = count(c, base, args);
            return new OrderPage(items, q.getPage(), q.getPageSize(), total);
        } catch (SQLException ex) {
            throw fail("查询订单失败", ex);
        }
    }

    @Override
    public boolean decrementStock(Connection c, long productId, int quantity) {
        String sql = "UPDATE products SET stock_qty = stock_qty - ?, version = version + 1 "
                + "WHERE id = ? AND status = 'ON_SALE' AND stock_qty >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw fail("扣减商品库存失败", ex);
        }
    }

    @Override
    public void incrementStock(Connection c, long productId, int quantity) {
        String sql = "UPDATE products SET stock_qty = stock_qty + ?, version = version + 1 WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, productId);
            if (ps.executeUpdate() != 1) throw new StoreRepositoryException("商品不存在");
        } catch (SQLException ex) {
            throw fail("恢复商品库存失败", ex);
        }
    }

    @Override
    public boolean updateOrderStatus(Connection c, long id, String status) {
        String sql = "UPDATE store_orders SET status = ?, "
                + "paid_at = CASE WHEN ? = 'PAID' THEN COALESCE(paid_at, CURRENT_TIMESTAMP(3)) ELSE paid_at END, "
                + "cancelled_at = CASE WHEN ? = 'CANCELLED' THEN CURRENT_TIMESTAMP(3) ELSE cancelled_at END, "
                + "completed_at = CASE WHEN ? = 'COMPLETED' THEN CURRENT_TIMESTAMP(3) ELSE completed_at END, "
                + "version = version + 1 WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setString(3, status);
            ps.setString(4, status);
            ps.setLong(5, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw fail("更新订单状态失败", ex);
        }
    }

    @Override public boolean updateOrderShipping(Connection c, long id, String status,
            String trackingNo, String remark) { return shipping.update(c, id, status, trackingNo, remark); }

    private static OrderDto readOrder(Connection c, ResultSet rs) throws SQLException {
        List<OrderItemDto> items = new ArrayList<OrderItemDto>();
        String sql = "SELECT product_id, product_name_snapshot, unit_price_snapshot, quantity, line_amount "
                + "FROM store_order_items WHERE order_id = ? ORDER BY product_id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, rs.getLong("id"));
            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) items.add(new OrderItemDto(rows.getLong("product_id"),
                        rows.getString("product_name_snapshot"), rows.getBigDecimal("unit_price_snapshot"),
                        rows.getInt("quantity"), rows.getBigDecimal("line_amount")));
            }
        }
        return new OrderDto(rs.getLong("id"), rs.getString("order_no"), rs.getLong("buyer_id"),
                rs.getBigDecimal("total_amount"), rs.getBigDecimal("original_amount"),
                rs.getBigDecimal("discount_amount"), rs.getString("promotion_code"),
                rs.getString("coupon_code"), rs.getString("payment_mode"), rs.getString("status"),
                rs.getString("shipping_status"), rs.getString("tracking_no"), rs.getString("shipping_remark"),
                MySqlStoreProductRepository.time(rs.getTimestamp("created_at")),
                MySqlStoreProductRepository.time(rs.getTimestamp("paid_at")),
                MySqlStoreProductRepository.time(rs.getTimestamp("cancelled_at")),
                MySqlStoreProductRepository.time(rs.getTimestamp("completed_at")), items);
    }

    private static long count(Connection c, String base, List<Object> args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*)" + base)) {
            bind(ps, args, 1); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); } }
    }

    private static int bind(PreparedStatement ps, List<Object> args, int index)
            throws SQLException {
        for (Object arg : args) {
            if (arg instanceof Long) ps.setLong(index++, ((Long) arg).longValue());
            else ps.setString(index++, String.valueOf(arg));
        }
        return index;
    }
    private static StoreRepositoryException fail(String message, Throwable cause) { return new StoreRepositoryException(message, cause); }
}
