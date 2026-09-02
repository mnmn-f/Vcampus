package edu.seu.vcampus.server.store.handler;

import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountRechargeRequest;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.OrderStatusUpdateRequest;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.dto.store.StockAdjustRequest;
import edu.seu.vcampus.common.dto.store.StoreIdRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.service.StoreService;
import edu.seu.vcampus.server.store.service.StoreServiceException;

/** 将商店命令适配到统一服务门面，并只接受明确的 Common DTO。 */
public final class StoreCommandHandler implements CommandHandler {
    private final String command;
    private final StoreService service;

    public StoreCommandHandler(String command, StoreService service) {
        if (command == null || command.trim().isEmpty() || service == null) {
            throw new IllegalArgumentException("store handler dependencies are required");
        }
        this.command = command;
        this.service = service;
    }

    @Override
    public Message handle(Message request, SessionContext session) {
        try {
            Object payload = request == null ? null : request.getPayload();
            if (StoreCommands.PRODUCT_SEARCH.equals(command)) {
                return Message.success(request, service.searchProducts(session,
                        payload == null ? null : require(payload, ProductQuery.class)));
            }
            if (StoreCommands.PRODUCT_DETAIL.equals(command)) {
                return Message.success(request, service.getProductDetail(session, id(payload)));
            }
            if (StoreCommands.PRODUCT_SAVE.equals(command)) {
                return Message.success(request, service.saveProduct(session,
                        require(payload, ProductWriteRequest.class)));
            }
            if (StoreCommands.PRODUCT_CREATE.equals(command)) {
                return Message.success(request, service.createProduct(session,
                        require(payload, ProductWriteRequest.class)));
            }
            if (StoreCommands.PRODUCT_UPDATE.equals(command)) {
                return Message.success(request, service.updateProduct(session,
                        require(payload, ProductWriteRequest.class)));
            }
            if (StoreCommands.PRODUCT_STOCK_ADJUST.equals(command)) {
                return Message.success(request, service.adjustProductStock(session,
                        require(payload, StockAdjustRequest.class)));
            }
            if (StoreCommands.CART_GET.equals(command)) return Message.success(request, service.getCart(session));
            if (StoreCommands.CART_ADD_ITEM.equals(command)) {
                return Message.success(request, service.addCartItem(session,
                        require(payload, CartItemRequest.class)));
            }
            if (StoreCommands.CART_UPDATE_ITEM.equals(command)) {
                return Message.success(request, service.updateCartItem(session,
                        require(payload, CartItemRequest.class)));
            }
            if (StoreCommands.CART_REMOVE_ITEM.equals(command)) {
                return Message.success(request, service.removeCartItem(session, id(payload)));
            }
            if (StoreCommands.ORDER_CREATE.equals(command)) {
                return Message.success(request, service.createOrder(session));
            }
            if (StoreCommands.ORDER_PAY.equals(command)) {
                return Message.success(request, service.payOrder(session,
                        require(payload, PaymentRequest.class)));
            }
            if (StoreCommands.ORDER_MINE.equals(command)) {
                return Message.success(request, service.getOwnOrders(session,
                        payload == null ? null : require(payload, OrderQuery.class)));
            }
            if (StoreCommands.ORDER_DETAIL.equals(command)) {
                return Message.success(request, service.getOrderDetail(session, id(payload)));
            }
            if (StoreCommands.ORDER_MANAGER_SEARCH.equals(command)) {
                return Message.success(request, service.searchOrders(session,
                        payload == null ? null : require(payload, OrderQuery.class)));
            }
            if (StoreCommands.ORDER_STATUS_UPDATE.equals(command)) {
                return Message.success(request, service.updateOrderStatus(session,
                        require(payload, OrderStatusUpdateRequest.class)));
            }
            if (StoreCommands.ACCOUNT_GET.equals(command)) return Message.success(request, service.getAccount(session));
            if (StoreCommands.ACCOUNT_LEDGER.equals(command)) {
                return Message.success(request, service.getAccountLedger(session,
                        payload == null ? null : require(payload, AccountLedgerQuery.class)));
            }
            if (StoreCommands.ACCOUNT_RECHARGE.equals(command)) {
                return Message.success(request, service.recharge(session,
                        require(payload, AccountRechargeRequest.class)));
            }
            if (StoreCommands.SALES_REPORT.equals(command)) {
                return Message.success(request, service.salesReport(session,
                        payload == null ? null : require(payload, StoreSalesQuery.class)));
            }
            return Message.failure(request, ResultCodes.INVALID_INPUT, "不支持的商店操作");
        } catch (StoreServiceException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        } catch (IllegalArgumentException ex) {
            return Message.failure(request, ResultCodes.INVALID_INPUT, "请求参数格式不正确");
        } catch (RuntimeException ex) {
            return Message.failure(request, ResultCodes.INTERNAL_ERROR, "商店服务暂时不可用");
        }
    }

    @Override
    public Permission requiredPermission() {
        if (StoreCommands.PRODUCT_SEARCH.equals(command)
                || StoreCommands.PRODUCT_DETAIL.equals(command)) return Permission.STORE_READ;
        if (StoreCommands.PRODUCT_SAVE.equals(command)
                || StoreCommands.PRODUCT_CREATE.equals(command)
                || StoreCommands.PRODUCT_UPDATE.equals(command)
                || StoreCommands.PRODUCT_STOCK_ADJUST.equals(command)) return Permission.STORE_MANAGE;
        if (StoreCommands.ORDER_MANAGER_SEARCH.equals(command)) return Permission.STORE_SALES_READ;
        if (StoreCommands.SALES_REPORT.equals(command)) return Permission.STORE_SALES_READ;
        if (StoreCommands.ORDER_DETAIL.equals(command)
                || StoreCommands.ORDER_STATUS_UPDATE.equals(command)) return null;
        return Permission.STORE_PURCHASE;
    }

    @Override
    public boolean requiresAuthentication() { return true; }

    private static long id(Object value) {
        if (value instanceof StoreIdRequest) return ((StoreIdRequest) value).getId();
        if (value instanceof Number) return ((Number) value).longValue();
        throw new IllegalArgumentException("id payload is required");
    }

    private static <T> T require(Object value, Class<T> type) {
        if (!type.isInstance(value)) throw new IllegalArgumentException("payload must be " + type.getSimpleName());
        return type.cast(value);
    }
}
