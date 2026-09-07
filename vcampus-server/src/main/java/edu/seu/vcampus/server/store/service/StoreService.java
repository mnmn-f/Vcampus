package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.CheckoutConfirmRequest;
import edu.seu.vcampus.common.dto.store.CheckoutPreviewDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountRechargeRequest;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
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
import edu.seu.vcampus.common.dto.store.StockAdjustRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.StoreCategoryWriteRequest;
import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import edu.seu.vcampus.common.dto.store.FriendPaymentDecisionRequest;
import edu.seu.vcampus.common.dto.store.FriendPaymentDto;
import edu.seu.vcampus.common.dto.store.FriendPaymentPage;
import edu.seu.vcampus.common.dto.store.FriendPaymentQuery;
import edu.seu.vcampus.common.dto.store.FriendPaymentRequest;
import edu.seu.vcampus.common.dto.store.StoreIdRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;
import edu.seu.vcampus.server.store.repository.StoreExperienceRepository;
import edu.seu.vcampus.server.store.repository.InMemoryStoreExperienceRepository;
import edu.seu.vcampus.server.store.repository.mysql.MySqlStoreExperienceRepository;

/** 商店命令处理器使用的统一服务门面。 */
public final class StoreService {
    private final StoreProductService products;
    private final StoreCartService cart;
    private final StoreOrderService orders;
    private final StoreOrderStatusService orderStatuses;
    private final StoreShippingService shipping;
    private final StorePaymentService payments;
    private final StoreAccountService accounts;
    private final StoreSalesService sales;
    private final StoreExperienceService experience;

    public StoreService(StoreRecordRepository repository, TransactionManager manager) {
        this(repository, new StoreTransactionManagerRunner(manager), experienceRepository(repository));
    }

    public StoreService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        this(repository, transactions, experienceRepository(repository));
    }

    public StoreService(StoreRecordRepository repository, StoreTransactionRunner transactions,
                        StoreExperienceRepository experienceRepository) {
        if (repository == null || transactions == null) {
            throw new IllegalArgumentException("store service dependencies required");
        }
        StoreTransactionRunner safeTransactions = transactions;
        if (repository instanceof InMemoryStoreRecordRepository) {
            safeTransactions = new SynchronizedStoreTransactionRunner(transactions,
                    ((InMemoryStoreRecordRepository) repository).transactionLock());
        }
        products = new StoreProductService(repository, safeTransactions);
        cart = new StoreCartService(repository, safeTransactions);
        orders = new StoreOrderService(repository, safeTransactions);
        orderStatuses = new StoreOrderStatusService(repository, safeTransactions);
        shipping = new StoreShippingService(repository, safeTransactions);
        payments = new StorePaymentService(repository, safeTransactions);
        accounts = new StoreAccountService(repository, safeTransactions);
        sales = new StoreSalesService(repository, safeTransactions);
        experience = new StoreExperienceService(repository, experienceRepository, safeTransactions);
    }

    public ProductPage searchProducts(SessionContext s, ProductQuery q) throws StoreServiceException {
        return products.search(s, q);
    }
    public ProductDto getProductDetail(SessionContext s, long id) throws StoreServiceException {
        return products.detail(s, id);
    }
    public ProductDto createProduct(SessionContext s, ProductWriteRequest r) throws StoreServiceException {
        return products.create(s, r);
    }
    public ProductDto updateProduct(SessionContext s, ProductWriteRequest r) throws StoreServiceException {
        return products.update(s, r);
    }
    public ProductDto saveProduct(SessionContext s, ProductWriteRequest r) throws StoreServiceException {
        return products.save(s, r);
    }
    public ProductDto adjustProductStock(SessionContext s, StockAdjustRequest r)
            throws StoreServiceException { return products.adjustStock(s, r); }

    public CartDto getCart(SessionContext s) throws StoreServiceException { return cart.get(s); }
    public CartDto addCartItem(SessionContext s, CartItemRequest r) throws StoreServiceException {
        return cart.add(s, r);
    }
    public CartDto updateCartItem(SessionContext s, CartItemRequest r) throws StoreServiceException {
        return cart.update(s, r);
    }
    public CartDto removeCartItem(SessionContext s, long productId) throws StoreServiceException {
        return cart.remove(s, productId);
    }

    public OrderDto createOrder(SessionContext s) throws StoreServiceException { return orders.create(s); }
    public OrderDto payOrder(SessionContext s, PaymentRequest r) throws StoreServiceException {
        return payments.pay(s, r);
    }
    public OrderPage getOwnOrders(SessionContext s, OrderQuery q) throws StoreServiceException {
        return orders.mine(s, q);
    }
    public OrderDto getOrderDetail(SessionContext s, long id) throws StoreServiceException {
        return orders.detail(s, id);
    }
    public OrderPage searchOrders(SessionContext s, OrderQuery q) throws StoreServiceException {
        return orders.managerSearch(s, q);
    }
    public OrderDto updateOrderStatus(SessionContext s, OrderStatusUpdateRequest r)
            throws StoreServiceException {
        return orderStatuses.update(s, r);
    }
    public OrderDto updateOrderShipping(SessionContext s, OrderShippingUpdateRequest r)
            throws StoreServiceException { return shipping.update(s, r); }

    public AccountDto getAccount(SessionContext s) throws StoreServiceException { return accounts.get(s); }
    public AccountLedgerPage getAccountLedger(SessionContext s, AccountLedgerQuery q)
            throws StoreServiceException { return accounts.ledger(s, q); }
    public AccountDto recharge(SessionContext s, AccountRechargeRequest r)
            throws StoreServiceException { return accounts.recharge(s, r); }
    public StoreSalesPage salesReport(SessionContext s, StoreSalesQuery q)
            throws StoreServiceException { return sales.report(s, q); }

    public StoreCategoryPage listCategories(SessionContext s) throws StoreServiceException {
        return experience.categories(s);
    }
    public StoreCategoryDto saveCategory(SessionContext s, StoreCategoryWriteRequest r)
            throws StoreServiceException { return experience.saveCategory(s, r); }
    public PromotionPage listPromotions(SessionContext s) throws StoreServiceException {
        return experience.promotions(s);
    }
    public PromotionDto savePromotion(SessionContext s, PromotionWriteRequest r)
            throws StoreServiceException { return experience.savePromotion(s, r); }
    public CouponPage listCoupons(SessionContext s) throws StoreServiceException {
        return experience.coupons(s);
    }
    public CouponDto claimCoupon(SessionContext s, CouponClaimRequest r)
            throws StoreServiceException { return experience.claim(s, r); }
    public CheckoutPreviewDto checkoutPreview(SessionContext s, String couponCode)
            throws StoreServiceException { return experience.checkout().preview(s, couponCode); }
    public OrderDto confirmCheckout(SessionContext s, CheckoutConfirmRequest r)
            throws StoreServiceException { return experience.checkout().confirm(s, r); }
    public ProductReviewPage listReviews(SessionContext s, ProductReviewQuery q)
            throws StoreServiceException { return experience.reviews(s, q); }
    public ProductReviewDto addReview(SessionContext s, ProductReviewWriteRequest r)
            throws StoreServiceException { return experience.addReview(s, r); }
    public FriendPaymentDto createFriendPayment(SessionContext s, FriendPaymentRequest r)
            throws StoreServiceException { return experience.createFriend(s, r); }
    public FriendPaymentPage listFriendPayments(SessionContext s, FriendPaymentQuery q)
            throws StoreServiceException { return experience.friendList(s, q); }
    public FriendPaymentDto withdrawFriendPayment(SessionContext s, StoreIdRequest r)
            throws StoreServiceException { return experience.withdrawFriend(s, r); }
    public FriendPaymentDto decideFriendPayment(SessionContext s, FriendPaymentDecisionRequest r)
            throws StoreServiceException { return experience.decideFriend(s, r); }
    public StoreSalesTrendPage salesTrend(SessionContext s, StoreSalesTrendQuery q)
            throws StoreServiceException { return experience.trend(s, q); }

    private static StoreExperienceRepository experienceRepository(StoreRecordRepository repository) {
        return repository instanceof InMemoryStoreRecordRepository
                ? new InMemoryStoreExperienceRepository(repository)
                : new MySqlStoreExperienceRepository();
    }
}
