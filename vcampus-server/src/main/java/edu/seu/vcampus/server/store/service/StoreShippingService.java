package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderShippingUpdateRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.sql.Connection;

/** 商店管理员维护订单物流，节点只允许向前推进。 */
final class StoreShippingService {
    private static final String[] FLOW = {"PREPARING", "SHIPPED", "IN_TRANSIT", "READY_FOR_PICKUP", "DELIVERED"};
    private final StoreRecordRepository repository;
    private final StoreTransactionRunner transactions;

    StoreShippingService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        this.repository = repository; this.transactions = transactions;
    }

    OrderDto update(final SessionContext session, final OrderShippingUpdateRequest request)
            throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_MANAGE);
        if (request == null) throw new StoreServiceException(ResultCodes.INVALID_INPUT, "物流参数不正确");
        StoreServiceSupport.positiveId(request.getOrderId(), "订单编号");
        final String target = StoreServiceSupport.required(request.getShippingStatus(), "物流状态").toUpperCase(java.util.Locale.ROOT);
        final int targetStep = step(target);
        StoreServiceSupport.maxLength(request.getTrackingNo(), 80, "物流单号");
        StoreServiceSupport.maxLength(request.getRemark(), 500, "物流说明");
        return StoreServiceSupport.inTransaction(transactions, new TransactionWork<OrderDto>() {
            @Override public OrderDto execute(Connection c) throws StoreServiceException {
                OrderDto order = repository.findOrder(c, request.getOrderId(), true);
                if (order == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "订单不存在");
                if (!"PAID".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus()))
                    throw new StoreServiceException(ResultCodes.CONFLICT, "只有已支付订单可以更新物流");
                if (order.getShippingStatus() != null && targetStep < step(order.getShippingStatus()))
                    throw new StoreServiceException(ResultCodes.CONFLICT, "物流状态不能回退");
                if (!repository.updateOrderShipping(c, order.getId(), target,
                        text(request.getTrackingNo()), text(request.getRemark())))
                    throw new StoreServiceException(ResultCodes.CONFLICT, "物流状态已变化");
                return repository.findOrder(c, order.getId(), false);
            }
        });
    }

    private static int step(String value) throws StoreServiceException {
        for (int i = 0; i < FLOW.length; i++) if (FLOW[i].equals(value)) return i;
        throw new StoreServiceException(ResultCodes.INVALID_INPUT, "物流状态不正确");
    }
    private static String text(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
