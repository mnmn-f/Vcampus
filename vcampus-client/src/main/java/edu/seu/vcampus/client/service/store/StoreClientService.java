package edu.seu.vcampus.client.service.store;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountRechargeRequest;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.OrderStatusUpdateRequest;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.dto.store.StockAdjustRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;

/** 商店页面使用的网络服务边界，不依赖 Swing 演示页面。 */
public interface StoreClientService {
    ProductPage searchProducts(ProductQuery query) throws NetworkClientException;
    ProductDto getProductDetail(long productId) throws NetworkClientException;
    ProductDto createProduct(ProductWriteRequest request) throws NetworkClientException;
    ProductDto updateProduct(ProductWriteRequest request) throws NetworkClientException;
    ProductDto saveProduct(ProductWriteRequest request) throws NetworkClientException;
    ProductDto adjustProductStock(StockAdjustRequest request) throws NetworkClientException;

    CartDto getCart() throws NetworkClientException;
    CartDto addCartItem(CartItemRequest request) throws NetworkClientException;
    CartDto updateCartItem(CartItemRequest request) throws NetworkClientException;
    CartDto removeCartItem(long productId) throws NetworkClientException;

    OrderDto createOrder() throws NetworkClientException;
    OrderDto payOrder(PaymentRequest request) throws NetworkClientException;
    OrderPage getOwnOrders(OrderQuery query) throws NetworkClientException;
    OrderDto getOrderDetail(long orderId) throws NetworkClientException;
    OrderPage searchOrders(OrderQuery query) throws NetworkClientException;
    OrderDto updateOrderStatus(OrderStatusUpdateRequest request) throws NetworkClientException;
    StoreSalesPage salesReport(StoreSalesQuery query) throws NetworkClientException;

    AccountDto getAccount() throws NetworkClientException;
    AccountLedgerPage getAccountLedger(AccountLedgerQuery query) throws NetworkClientException;
    AccountDto recharge(AccountRechargeRequest request) throws NetworkClientException;
}
