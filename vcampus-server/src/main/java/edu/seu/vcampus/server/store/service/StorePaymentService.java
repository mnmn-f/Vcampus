package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.OrderStatus;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.LedgerRecord;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.math.BigDecimal;
import java.sql.Connection;

/** 余额支付事务：锁订单、账户和商品，原子完成库存/余额/流水/订单状态。 */
final class StorePaymentService {
    private final StoreRecordRepository repository;
    private final StoreTransactionRunner transactions;

    StorePaymentService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    OrderDto pay(final SessionContext session, final PaymentRequest request)
            throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_PURCHASE);
        validate(request);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<OrderDto>() {
                    @Override public OrderDto execute(Connection c) throws StoreServiceException {
                        OrderDto order = order(c, request.getOrderId(), true);
                        if (order.getBuyerId() != session.getUserId()) {
                            throw new StoreServiceException(ResultCodes.NOT_FOUND, "订单不存在");
                        }
                        AccountDto account = account(c, session.getUserId());
                        LedgerRecord old = repository.findTransactionByKey(c,
                                request.getIdempotencyKey());
                        if (old != null) return idempotent(c, order, account, old);
                        if (!OrderStatus.CREATED.name().equalsIgnoreCase(order.getStatus())) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "订单已支付或不可支付");
                        }
                        BigDecimal total = checkProducts(c, order);
                        if (account.getBalance().compareTo(total) < 0) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "账户余额不足");
                        }
                        decrement(c, order);
                        BigDecimal updated = account.getBalance().subtract(total);
                        if (!repository.updateAccountBalance(c, account.getId(), account.getBalance(), updated)) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "账户余额已变化，请重试");
                        }
                        repository.insertTransaction(c, account.getId(), "PURCHASE", total.negate(),
                                account.getBalance(), updated, "STORE_ORDER", order.getId(),
                                request.getIdempotencyKey(), session.getUserId(), request.getRemark());
                        if (!repository.updateOrderStatus(c, order.getId(), OrderStatus.PAID.name())) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "订单状态已变化");
                        }
                        return order(c, order.getId(), false);
                    }
                });
    }

    private BigDecimal checkProducts(Connection c, OrderDto order)
            throws StoreServiceException {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemDto item : order.getItems()) {
            StoreServiceSupport.quantity(item.getQuantity());
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0
                    || item.getLineAmount() == null) {
                throw new StoreServiceException(ResultCodes.CONFLICT, "订单价格无效");
            }
            BigDecimal expected = item.getUnitPrice().multiply(
                    BigDecimal.valueOf(item.getQuantity()));
            if (expected.compareTo(item.getLineAmount()) != 0) {
                throw new StoreServiceException(ResultCodes.CONFLICT, "订单明细金额校验失败");
            }
            ProductDto product = repository.findProduct(c, item.getProductId(), true);
            if (product == null || !"ON_SALE".equalsIgnoreCase(product.getStatus())) {
                throw new StoreServiceException(ResultCodes.CONFLICT, "订单中有商品已下架");
            }
            total = total.add(expected);
        }
        if (order.getItems().isEmpty() || order.getTotalAmount() == null
                || total.compareTo(order.getTotalAmount()) != 0) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "订单金额校验失败");
        }
        return total;
    }

    private void decrement(Connection c, OrderDto order) throws StoreServiceException {
        for (OrderItemDto item : order.getItems()) {
            if (!repository.decrementStock(c, item.getProductId(), item.getQuantity())) {
                throw new StoreServiceException(ResultCodes.CONFLICT, "商品库存不足");
            }
        }
    }

    private OrderDto idempotent(Connection c, OrderDto order, AccountDto account,
                                LedgerRecord old) throws StoreServiceException {
        if (old.getAccountId() != account.getId()
                || old.getReferenceId() == null || old.getReferenceId().longValue() != order.getId()
                || !"PURCHASE".equals(old.getTransactionType())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "幂等键已用于其他操作");
        }
        if (!OrderStatus.PAID.name().equalsIgnoreCase(order.getStatus())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "幂等流水与订单状态不一致");
        }
        return order;
    }

    private AccountDto account(Connection c, long userId) throws StoreServiceException {
        AccountDto found = repository.findAccount(c, userId, true);
        if (found == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "校园账户不存在");
        if (!"ACTIVE".equalsIgnoreCase(found.getStatus())) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "校园账户不可支付");
        }
        return found;
    }

    private OrderDto order(Connection c, long id, boolean lock) throws StoreServiceException {
        OrderDto found = repository.findOrder(c, id, lock);
        if (found == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "订单不存在");
        return found;
    }

    private static void validate(PaymentRequest request) throws StoreServiceException {
        if (request == null) throw new StoreServiceException(ResultCodes.INVALID_INPUT, "支付参数不正确");
        StoreServiceSupport.positiveId(request.getOrderId(), "订单编号");
        StoreAccountService.validateKey(request.getIdempotencyKey());
        StoreServiceSupport.maxLength(request.getRemark(), 500, "备注");
    }
}
