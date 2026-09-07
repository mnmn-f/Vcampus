package edu.seu.vcampus.client.service.store;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountRechargeRequest;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.CheckoutConfirmRequest;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewRequest;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewDto;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentDecisionRequest;
import edu.seu.vcampus.common.dto.store.FriendPaymentDto;
import edu.seu.vcampus.common.dto.store.FriendPaymentPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentQuery;
import edu.seu.vcampus.common.dto.store.FriendPaymentRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.OrderStatusUpdateRequest;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.StoreCategoryWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreIdRequest;
import edu.seu.vcampus.common.dto.store.StockAdjustRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.StoreCommands;

import java.io.Serializable;

/** 商店网络客户端；只负责命令、会话令牌和 DTO 响应映射。 */
public final class NetworkStoreClientService implements StoreClientService {
    private final NetworkClientService network;
    private final ClientSession session;

    public NetworkStoreClientService(NetworkClientService network, ClientSession session) {
        if (network == null || session == null) throw new IllegalArgumentException("store client dependencies required");
        this.network = network;
        this.session = session;
    }

    public NetworkStoreClientService(ClientGateway gateway, ClientSession session) {
        this(new NetworkClientService(gateway), session);
    }

    @Override public ProductPage searchProducts(ProductQuery q) throws NetworkClientException {
        return payload(StoreCommands.PRODUCT_SEARCH, q, ProductPage.class);
    }
    @Override public ProductDto getProductDetail(long id) throws NetworkClientException {
        return payload(StoreCommands.PRODUCT_DETAIL, new StoreIdRequest(id), ProductDto.class);
    }
    @Override public ProductDto createProduct(ProductWriteRequest r) throws NetworkClientException {
        return payload(StoreCommands.PRODUCT_CREATE, r, ProductDto.class);
    }
    @Override public ProductDto updateProduct(ProductWriteRequest r) throws NetworkClientException {
        return payload(StoreCommands.PRODUCT_UPDATE, r, ProductDto.class);
    }
    @Override public ProductDto saveProduct(ProductWriteRequest r) throws NetworkClientException {
        return payload(StoreCommands.PRODUCT_SAVE, r, ProductDto.class);
    }
    @Override public ProductDto adjustProductStock(StockAdjustRequest r) throws NetworkClientException {
        return payload(StoreCommands.PRODUCT_STOCK_ADJUST, r, ProductDto.class);
    }
    @Override public StoreCategoryPage listCategories() throws NetworkClientException {
        return payload(StoreCommands.CATEGORY_LIST, null, StoreCategoryPage.class);
    }
    @Override public StoreCategoryDto saveCategory(StoreCategoryWriteRequest r) throws NetworkClientException {
        return payload(StoreCommands.CATEGORY_SAVE, r, StoreCategoryDto.class);
    }

    @Override public CartDto getCart() throws NetworkClientException {
        return payload(StoreCommands.CART_GET, null, CartDto.class);
    }
    @Override public CartDto addCartItem(CartItemRequest r) throws NetworkClientException {
        return payload(StoreCommands.CART_ADD_ITEM, r, CartDto.class);
    }
    @Override public CartDto updateCartItem(CartItemRequest r) throws NetworkClientException {
        return payload(StoreCommands.CART_UPDATE_ITEM, r, CartDto.class);
    }
    @Override public CartDto removeCartItem(long id) throws NetworkClientException {
        return payload(StoreCommands.CART_REMOVE_ITEM,
                new StoreIdRequest(id), CartDto.class);
    }

    @Override public OrderDto createOrder() throws NetworkClientException {
        return payload(StoreCommands.ORDER_CREATE, null, OrderDto.class);
    }
    @Override public OrderDto payOrder(PaymentRequest r) throws NetworkClientException {
        return payload(StoreCommands.ORDER_PAY, r, OrderDto.class);
    }
    @Override public CheckoutPreviewDto checkoutPreview(String couponCode) throws NetworkClientException {
        return payload(StoreCommands.CHECKOUT_PREVIEW, new CheckoutPreviewRequest(couponCode), CheckoutPreviewDto.class);
    }
    @Override public OrderDto confirmCheckout(CheckoutConfirmRequest r) throws NetworkClientException {
        return payload(StoreCommands.CHECKOUT_CONFIRM, r, OrderDto.class);
    }
    @Override public OrderPage getOwnOrders(OrderQuery q) throws NetworkClientException {
        return payload(StoreCommands.ORDER_MINE, q, OrderPage.class);
    }
    @Override public OrderDto getOrderDetail(long id) throws NetworkClientException {
        return payload(StoreCommands.ORDER_DETAIL,
                new StoreIdRequest(id), OrderDto.class);
    }
    @Override public OrderPage searchOrders(OrderQuery q) throws NetworkClientException {
        return payload(StoreCommands.ORDER_MANAGER_SEARCH, q, OrderPage.class);
    }
    @Override public OrderDto updateOrderStatus(OrderStatusUpdateRequest r) throws NetworkClientException {
        return payload(StoreCommands.ORDER_STATUS_UPDATE, r, OrderDto.class);
    }
    @Override public StoreSalesPage salesReport(StoreSalesQuery q) throws NetworkClientException {
        return payload(StoreCommands.SALES_REPORT, q, StoreSalesPage.class);
    }
    @Override public StoreSalesTrendPage salesTrend(StoreSalesTrendQuery q) throws NetworkClientException {
        return payload(StoreCommands.SALES_TREND, q, StoreSalesTrendPage.class);
    }
    @Override public PromotionPage listPromotions() throws NetworkClientException {
        return payload(StoreCommands.PROMOTION_LIST, null, PromotionPage.class);
    }
    @Override public PromotionDto savePromotion(PromotionWriteRequest r) throws NetworkClientException {
        return payload(StoreCommands.PROMOTION_SAVE, r, PromotionDto.class);
    }
    @Override public CouponPage listCoupons() throws NetworkClientException {
        return payload(StoreCommands.COUPON_MINE, null, CouponPage.class);
    }
    @Override public CouponDto claimCoupon(CouponClaimRequest r) throws NetworkClientException {
        return payload(StoreCommands.COUPON_CLAIM, r, CouponDto.class);
    }
    @Override public ProductReviewPage listReviews(ProductReviewQuery q) throws NetworkClientException {
        return payload(StoreCommands.REVIEW_LIST, q, ProductReviewPage.class);
    }
    @Override public ProductReviewDto addReview(ProductReviewWriteRequest r) throws NetworkClientException {
        return payload(StoreCommands.REVIEW_CREATE, r, ProductReviewDto.class);
    }
    @Override public FriendPaymentDto createFriendPayment(FriendPaymentRequest r) throws NetworkClientException {
        return payload(StoreCommands.FRIEND_PAY_CREATE, r, FriendPaymentDto.class);
    }
    @Override public FriendPaymentPage listFriendPayments(FriendPaymentQuery q) throws NetworkClientException {
        return payload(StoreCommands.FRIEND_PAY_MINE, q, FriendPaymentPage.class);
    }
    @Override public FriendPaymentDto withdrawFriendPayment(long id) throws NetworkClientException {
        return payload(StoreCommands.FRIEND_PAY_WITHDRAW, new StoreIdRequest(id), FriendPaymentDto.class);
    }
    @Override public FriendPaymentDto decideFriendPayment(FriendPaymentDecisionRequest r) throws NetworkClientException {
        return payload(StoreCommands.FRIEND_PAY_DECIDE, r, FriendPaymentDto.class);
    }

    @Override public AccountDto getAccount() throws NetworkClientException {
        return payload(StoreCommands.ACCOUNT_GET, null, AccountDto.class);
    }
    @Override public AccountLedgerPage getAccountLedger(AccountLedgerQuery q)
            throws NetworkClientException {
        return payload(StoreCommands.ACCOUNT_LEDGER, q, AccountLedgerPage.class);
    }
    @Override public AccountDto recharge(AccountRechargeRequest r) throws NetworkClientException {
        return payload(StoreCommands.ACCOUNT_RECHARGE, r, AccountDto.class);
    }

    public void synchronizeSessionToken() {
        network.setSessionToken(session.isAuthenticated() ? session.getSessionToken() : null);
    }

    private <T> T payload(String command, Serializable body, Class<T> type)
            throws NetworkClientException {
        if (!session.isAuthenticated()) throw new NetworkClientException(ResultCodes.UNAUTHORIZED, "请先登录");
        network.setSessionToken(session.getSessionToken());
        Message response = network.request(command, body);
        Object value = response == null ? null : response.getPayload();
        if (value == null || !type.isInstance(value)) {
            throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "商店响应格式不正确");
        }
        return type.cast(value);
    }
}
