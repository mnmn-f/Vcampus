package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.OrderStatus;
import edu.seu.vcampus.common.dto.store.OrderStatusUpdateRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.LedgerRecord;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.math.BigDecimal;
import java.sql.Connection;

/** 管理员/学生订单状态流转，退款在同一事务写余额、流水与库存。 */
final class StoreOrderStatusService {
    private final StoreRecordRepository repository;
    private final StoreTransactionRunner transactions;

    StoreOrderStatusService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    OrderDto update(final SessionContext session, final OrderStatusUpdateRequest request)
            throws StoreServiceException {
        validate(request);
        if (session != null && session.getActiveRole() == Role.STORE_MANAGER) {
            StoreServiceSupport.requirePermission(session, Permission.STORE_MANAGE);
            return manager(session, request);
        }
        StoreOrderService.purchasePermission(session);
        String target = normalized(request.getStatus());
        if (!OrderStatus.CANCELLED.name().equals(target)) {
            throw new StoreServiceException(ResultCodes.FORBIDDEN, "学生只能取消本人待支付订单");
        }
        return cancelOwn(session, request.getOrderId());
    }

    private OrderDto manager(final SessionContext session,
                             final OrderStatusUpdateRequest request)
            throws StoreServiceException {
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<OrderDto>() {
                    @Override public OrderDto execute(Connection c) throws StoreServiceException {
                        OrderDto value = order(c, request.getOrderId(), true);
                        String target = normalized(request.getStatus());
                        if (OrderStatus.CANCELLED.name().equals(target)) require(value, OrderStatus.CREATED);
                        else if (OrderStatus.COMPLETED.name().equals(target)) require(value, OrderStatus.PAID);
                        else if (OrderStatus.REFUNDED.name().equals(target)) {
                            requireRefund(value);
                            refund(c, value, session.getUserId());
                            return order(c, value.getId(), false);
                        } else throw new StoreServiceException(ResultCodes.INVALID_INPUT, "不支持的订单状态流转");
                        update(c, value.getId(), target);
                        return order(c, value.getId(), false);
                    }
                });
    }

    private OrderDto cancelOwn(final SessionContext session, final long id)
            throws StoreServiceException {
        StoreServiceSupport.positiveId(id, "订单编号");
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<OrderDto>() {
                    @Override public OrderDto execute(Connection c) throws StoreServiceException {
                        OrderDto value = order(c, id, true);
                        if (value.getBuyerId() != session.getUserId()) {
                            throw new StoreServiceException(ResultCodes.NOT_FOUND, "订单不存在");
                        }
                        require(value, OrderStatus.CREATED);
                        update(c, id, OrderStatus.CANCELLED.name());
                        return order(c, id, false);
                    }
                });
    }

    private void refund(Connection c, OrderDto order, long operator)
            throws StoreServiceException {
        AccountDto account = repository.findOrderPaymentAccount(c, order.getId(), true);
        if (account == null && "FRIEND".equalsIgnoreCase(order.getPaymentMode())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "未找到好友代付流水，不能退款");
        }
        if (account == null) account = account(c, order.getBuyerId());
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "原付款账户不可退款");
        }
        String key = "STORE-REFUND-" + order.getId();
        LedgerRecord old = repository.findTransactionByKey(c, key);
        if (old != null && (old.getAccountId() != account.getId()
                || old.getReferenceId() == null
                || old.getReferenceId().longValue() != order.getId()
                || !"REFUND".equals(old.getTransactionType()))) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "退款幂等流水不一致");
        }
        if (old == null) {
            BigDecimal updated = account.getBalance().add(order.getTotalAmount());
            if (!repository.updateAccountBalance(c, account.getId(), account.getBalance(), updated)) {
                throw new StoreServiceException(ResultCodes.CONFLICT, "账户余额已变化，请重试");
            }
            repository.insertTransaction(c, account.getId(), "REFUND", order.getTotalAmount(),
                    account.getBalance(), updated, "STORE_ORDER", order.getId(), key, operator, "订单退款");
            for (OrderItemDto item : order.getItems()) repository.incrementStock(c,
                    item.getProductId(), item.getQuantity());
        }
        update(c, order.getId(), OrderStatus.REFUNDED.name());
    }

    private AccountDto account(Connection c, long userId) throws StoreServiceException {
        AccountDto found = repository.findAccount(c, userId, true);
        if (found == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "校园账户不存在");
        if (!"ACTIVE".equalsIgnoreCase(found.getStatus())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "校园账户不可退款");
        }
        return found;
    }

    private OrderDto order(Connection c, long id, boolean lock) throws StoreServiceException {
        OrderDto found = repository.findOrder(c, id, lock);
        if (found == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "订单不存在");
        return found;
    }

    private void update(Connection c, long id, String status) throws StoreServiceException {
        if (!repository.updateOrderStatus(c, id, status)) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "订单状态已变化");
        }
    }

    private static void validate(OrderStatusUpdateRequest request) throws StoreServiceException {
        if (request == null) throw new StoreServiceException(ResultCodes.INVALID_INPUT, "订单状态参数不正确");
        StoreServiceSupport.positiveId(request.getOrderId(), "订单编号");
        StoreServiceSupport.required(request.getStatus(), "订单状态");
        StoreServiceSupport.maxLength(request.getRemark(), 500, "备注");
    }

    private static String normalized(String value) throws StoreServiceException {
        String status = StoreServiceSupport.required(value, "订单状态").toUpperCase(java.util.Locale.ROOT);
        try { OrderStatus.valueOf(status); }
        catch (IllegalArgumentException ex) { throw new StoreServiceException(ResultCodes.INVALID_INPUT, "订单状态不正确"); }
        return status;
    }

    private static void require(OrderDto value, OrderStatus expected) throws StoreServiceException {
        if (!expected.name().equalsIgnoreCase(value.getStatus())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "订单当前状态不允许此操作");
        }
    }

    private static void requireRefund(OrderDto value) throws StoreServiceException {
        if (!OrderStatus.PAID.name().equalsIgnoreCase(value.getStatus())
                && !OrderStatus.COMPLETED.name().equalsIgnoreCase(value.getStatus())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "订单当前不可退款");
        }
    }
}
