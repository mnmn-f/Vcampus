package edu.seu.vcampus.server.store.service;

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
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

/** 商店命令处理器使用的统一服务门面。 */
public final class StoreService {
    private final StoreProductService products;
    private final StoreCartService cart;
    private final StoreOrderService orders;
    private final StoreOrderStatusService orderStatuses;
    private final StorePaymentService payments;
    private final StoreAccountService accounts;
    private final StoreSalesService sales;

    public StoreService(StoreRecordRepository repository, TransactionManager manager) {
        this(repository, new StoreTransactionManagerRunner(manager));
    }

    public StoreService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
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
        payments = new StorePaymentService(repository, safeTransactions);
        accounts = new StoreAccountService(repository, safeTransactions);
        sales = new StoreSalesService(repository, safeTransactions);
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

    public AccountDto getAccount(SessionContext s) throws StoreServiceException { return accounts.get(s); }
    public AccountLedgerPage getAccountLedger(SessionContext s, AccountLedgerQuery q)
            throws StoreServiceException { return accounts.ledger(s, q); }
    public AccountDto recharge(SessionContext s, AccountRechargeRequest r)
            throws StoreServiceException { return accounts.recharge(s, r); }
    public StoreSalesPage salesReport(SessionContext s, StoreSalesQuery q)
            throws StoreServiceException { return sales.report(s, q); }
}
