package edu.seu.vcampus.client.service.store;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountRechargeRequest;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.CheckoutConfirmRequest;
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
import edu.seu.vcampus.common.dto.store.OrderShippingUpdateRequest;
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
import edu.seu.vcampus.common.dto.store.StockAdjustRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;

/** 商店页面使用的网络服务边界，不依赖 Swing 演示页面。 */
public interface StoreClientService {
    byte[] getProductImage(String reference) throws NetworkClientException;
    edu.seu.vcampus.common.dto.store.ReviewCandidatePage reviewCandidates(ProductReviewQuery query) throws NetworkClientException;
    ProductPage searchProducts(ProductQuery query) throws NetworkClientException;
    ProductDto getProductDetail(long productId) throws NetworkClientException;
    ProductDto createProduct(ProductWriteRequest request) throws NetworkClientException;
    ProductDto updateProduct(ProductWriteRequest request) throws NetworkClientException;
    ProductDto saveProduct(ProductWriteRequest request) throws NetworkClientException;
    ProductDto adjustProductStock(StockAdjustRequest request) throws NetworkClientException;
    StoreCategoryPage listCategories() throws NetworkClientException;
    StoreCategoryDto saveCategory(StoreCategoryWriteRequest request) throws NetworkClientException;

    CartDto getCart() throws NetworkClientException;
    CartDto addCartItem(CartItemRequest request) throws NetworkClientException;
    CartDto updateCartItem(CartItemRequest request) throws NetworkClientException;
    CartDto removeCartItem(long productId) throws NetworkClientException;

    OrderDto createOrder() throws NetworkClientException;
    OrderDto payOrder(PaymentRequest request) throws NetworkClientException;
    CheckoutPreviewDto checkoutPreview(String couponCode) throws NetworkClientException;
    OrderDto confirmCheckout(CheckoutConfirmRequest request) throws NetworkClientException;
    OrderPage getOwnOrders(OrderQuery query) throws NetworkClientException;
    OrderDto getOrderDetail(long orderId) throws NetworkClientException;
    OrderPage searchOrders(OrderQuery query) throws NetworkClientException;
    OrderDto updateOrderStatus(OrderStatusUpdateRequest request) throws NetworkClientException;
    OrderDto updateOrderShipping(OrderShippingUpdateRequest request) throws NetworkClientException;
    StoreSalesPage salesReport(StoreSalesQuery query) throws NetworkClientException;
    StoreSalesTrendPage salesTrend(StoreSalesTrendQuery query) throws NetworkClientException;
    PromotionPage listPromotions() throws NetworkClientException;
    PromotionDto savePromotion(PromotionWriteRequest request) throws NetworkClientException;
    CouponPage listCoupons() throws NetworkClientException;
    CouponDto claimCoupon(CouponClaimRequest request) throws NetworkClientException;
    ProductReviewPage listReviews(ProductReviewQuery query) throws NetworkClientException;
    ProductReviewDto addReview(ProductReviewWriteRequest request) throws NetworkClientException;
    FriendPaymentDto createFriendPayment(FriendPaymentRequest request) throws NetworkClientException;
    FriendPaymentPage listFriendPayments(FriendPaymentQuery query) throws NetworkClientException;
    FriendPaymentDto withdrawFriendPayment(long requestId) throws NetworkClientException;
    FriendPaymentDto decideFriendPayment(FriendPaymentDecisionRequest request) throws NetworkClientException;

    AccountDto getAccount() throws NetworkClientException;
    AccountLedgerPage getAccountLedger(AccountLedgerQuery query) throws NetworkClientException;
    AccountDto recharge(AccountRechargeRequest request) throws NetworkClientException;
}
