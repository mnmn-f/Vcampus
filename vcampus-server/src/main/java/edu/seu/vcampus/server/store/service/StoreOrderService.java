package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.CartLine;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

/** 订单创建、本人/管理员查询及状态机。支付扣款由 StorePaymentService 负责。 */
final class StoreOrderService {
    private final StoreRecordRepository repository;
    private final StoreTransactionRunner transactions;

    StoreOrderService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    OrderDto create(final SessionContext session) throws StoreServiceException {
        purchasePermission(session);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<OrderDto>() {
                    @Override public OrderDto execute(Connection c) throws StoreServiceException {
                        List<CartLine> lines = repository.findCartLines(c, session.getUserId(), true);
                        if (lines.isEmpty()) throw new StoreServiceException(ResultCodes.CONFLICT, "购物车为空");
                        BigDecimal total = BigDecimal.ZERO;
                        for (CartLine line : lines) {
                            validateLine(line);
                            total = total.add(line.lineAmount());
                        }
                        long id = repository.insertOrder(c, session.getUserId(), orderNo(), total);
                        repository.insertOrderItems(c, id, lines);
                        repository.updateOrderPricing(c, id, total, BigDecimal.ZERO,
                                null, null, "SELF");
                        repository.clearCart(c, session.getUserId());
                        return order(c, id, false);
                    }
                });
    }

    OrderPage mine(final SessionContext session, OrderQuery query)
            throws StoreServiceException {
        purchasePermission(session);
        final OrderQuery safe = query == null ? new OrderQuery() : query;
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<OrderPage>() {
                    @Override public OrderPage execute(Connection c) {
                        return repository.findOrders(c, Long.valueOf(session.getUserId()), safe);
                    }
                });
    }

    OrderDto detail(final SessionContext session, final long orderId)
            throws StoreServiceException {
        detailPermission(session);
        StoreServiceSupport.positiveId(orderId, "订单编号");
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<OrderDto>() {
                    @Override public OrderDto execute(Connection c) throws StoreServiceException {
                        OrderDto value = order(c, orderId, false);
                        if (session.getActiveRole() != Role.STORE_MANAGER
                                && value.getBuyerId() != session.getUserId()) {
                            throw new StoreServiceException(ResultCodes.NOT_FOUND, "订单不存在");
                        }
                        return value;
                    }
                });
    }

    OrderPage managerSearch(final SessionContext session, OrderQuery query)
            throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_SALES_READ);
        StoreServiceSupport.requireRole(session, Role.STORE_MANAGER);
        final OrderQuery safe = query == null ? new OrderQuery() : query;
        if (safe.getBuyerId() != null && safe.getBuyerId().longValue() <= 0L) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "买家编号不正确");
        }
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<OrderPage>() {
                    @Override public OrderPage execute(Connection c) {
                        return repository.findOrders(c, safe.getBuyerId(), safe);
                    }
                });
    }

    OrderDto order(Connection c, long id, boolean lock) throws StoreServiceException {
        OrderDto found = repository.findOrder(c, id, lock);
        if (found == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "订单不存在");
        return found;
    }

    static void validateLine(CartLine line) throws StoreServiceException {
        StoreServiceSupport.quantity(line.getQuantity());
        if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "商品价格无效");
        }
        if (!"ON_SALE".equalsIgnoreCase(line.getProductStatus())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "商品当前不可购买");
        }
    }

    private static String orderNo() {
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        return "VC-" + System.currentTimeMillis() + "-" + token.substring(0, 12);
    }

    static void detailPermission(SessionContext session) throws StoreServiceException {
        if (session != null && session.getActiveRole() == Role.STORE_MANAGER) {
            StoreServiceSupport.requirePermission(session, Permission.STORE_SALES_READ);
        } else purchasePermission(session);
    }

    static void purchasePermission(SessionContext session) throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_PURCHASE);
    }

}
