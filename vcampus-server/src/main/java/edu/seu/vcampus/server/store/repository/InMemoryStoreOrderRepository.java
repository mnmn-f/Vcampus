package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.ProductDto;

import java.math.BigDecimal;
import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 订单和库存内存仓储，状态修改受共享状态锁保护。 */
final class InMemoryStoreOrderRepository implements StoreOrderRepository {
    private final InMemoryStoreState state;

    InMemoryStoreOrderRepository(InMemoryStoreState state) { this.state = state; }

    @Override
    public long insertOrder(Connection c, long buyerId, String orderNo, BigDecimal total) {
        synchronized (state) {
            for (InMemoryStoreState.MemoryOrder old : state.orders.values()) {
                if (old.orderNo.equals(orderNo)) throw new StoreRepositoryException("订单号已存在");
            }
            long id = state.orderSequence++;
            state.orders.put(id, new InMemoryStoreState.MemoryOrder(id, orderNo, buyerId, total));
            return id;
        }
    }

    @Override
    public void insertOrderItems(Connection c, long orderId, List<CartLine> lines) {
        synchronized (state) {
            InMemoryStoreState.MemoryOrder order = order(orderId);
            for (CartLine line : lines) order.items.add(line.toOrderItem());
        }
    }

    @Override public void updateOrderPricing(Connection c, long id, BigDecimal original,
            BigDecimal discount, String promotion, String coupon, String mode) {
        synchronized (state) {
            InMemoryStoreState.MemoryOrder order = order(id);
            order.originalAmount = original;
            order.discountAmount = discount == null ? BigDecimal.ZERO : discount;
            order.promotionCode = promotion;
            order.couponCode = coupon;
            order.paymentMode = mode == null ? "SELF" : mode;
        }
    }

    @Override
    public OrderDto findOrder(Connection c, long id, boolean forUpdate) {
        synchronized (state) {
            InMemoryStoreState.MemoryOrder order = state.orders.get(id);
            return order == null ? null : toDto(order);
        }
    }

    @Override
    public OrderPage findOrders(Connection c, Long buyerId, OrderQuery query) {
        OrderQuery q = query == null ? new OrderQuery() : query;
        synchronized (state) {
            List<OrderDto> found = new ArrayList<OrderDto>();
            for (InMemoryStoreState.MemoryOrder order : state.orders.values()) {
                if (buyerId != null && order.buyerId != buyerId.longValue()) continue;
                if (q.getOrderNo() != null && !order.orderNo.contains(q.getOrderNo())) continue;
                if (q.getStatus() != null && !q.getStatus().equalsIgnoreCase(order.status)) continue;
                found.add(toDto(order));
            }
            Collections.sort(found, new Comparator<OrderDto>() {
                @Override public int compare(OrderDto a, OrderDto b) {
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                }
            });
            int from = Math.min(q.getOffset(), found.size());
            int to = Math.min(from + q.getPageSize(), found.size());
            return new OrderPage(new ArrayList<OrderDto>(found.subList(from, to)),
                    q.getPage(), q.getPageSize(), found.size());
        }
    }

    @Override
    public boolean decrementStock(Connection c, long productId, int quantity) {
        synchronized (state) {
            ProductDto p = state.products.get(productId);
            if (p == null || !"ON_SALE".equalsIgnoreCase(p.getStatus())
                    || p.getStockQty() < quantity) return false;
            replaceProduct(p, p.getStockQty() - quantity);
            return true;
        }
    }

    @Override
    public void incrementStock(Connection c, long productId, int quantity) {
        synchronized (state) {
            ProductDto p = state.products.get(productId);
            if (p == null) throw new StoreRepositoryException("商品不存在");
            replaceProduct(p, p.getStockQty() + quantity);
        }
    }

    @Override
    public boolean updateOrderStatus(Connection c, long orderId, String status) {
        synchronized (state) {
            InMemoryStoreState.MemoryOrder order = state.orders.get(orderId);
            if (order == null) return false;
            order.status = status;
            LocalDateTime now = LocalDateTime.now();
            if ("PAID".equals(status)) order.paidAt = now;
            if ("CANCELLED".equals(status)) order.cancelledAt = now;
            if ("COMPLETED".equals(status)) order.completedAt = now;
            return true;
        }
    }

    void addOrder(OrderDto value) {
        synchronized (state) {
            LocalDateTime created = value.getCreatedAt() == null
                    ? LocalDateTime.now() : value.getCreatedAt();
            InMemoryStoreState.MemoryOrder order = new InMemoryStoreState.MemoryOrder(value.getId(),
                    value.getOrderNo(), value.getBuyerId(), value.getTotalAmount(), created);
            order.status = value.getStatus();
            order.originalAmount = value.getOriginalAmount();
            order.discountAmount = value.getDiscountAmount();
            order.promotionCode = value.getPromotionCode();
            order.couponCode = value.getCouponCode();
            order.paymentMode = value.getPaymentMode();
            order.paidAt = value.getPaidAt();
            order.cancelledAt = value.getCancelledAt();
            order.completedAt = value.getCompletedAt();
            order.items.addAll(value.getItems());
            state.orders.put(order.id, order);
            state.orderSequence = Math.max(state.orderSequence, order.id + 1L);
        }
    }

    private InMemoryStoreState.MemoryOrder order(long id) {
        InMemoryStoreState.MemoryOrder found = state.orders.get(id);
        if (found == null) throw new StoreRepositoryException("订单不存在");
        return found;
    }

    private static OrderDto toDto(InMemoryStoreState.MemoryOrder o) {
        return new OrderDto(o.id, o.orderNo, o.buyerId, o.totalAmount, o.originalAmount,
                o.discountAmount, o.promotionCode, o.couponCode, o.paymentMode, o.status,
                o.createdAt, o.paidAt, o.cancelledAt, o.completedAt,
                new ArrayList<OrderItemDto>(o.items));
    }

    private void replaceProduct(ProductDto old, int stock) {
        state.products.put(old.getId(), state.withStock(old, stock));
    }
}
