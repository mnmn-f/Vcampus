package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountTransactionDto;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.ProductDto;

import java.math.BigDecimal;

/** 商店内存仓储总入口；测试可直接补充商品、订单和校园账户。 */
public class InMemoryStoreRecordRepository extends DelegatingStoreRecordRepository {
    private final InMemoryStoreState state;
    private final InMemoryStoreProductRepository productStore;
    private final InMemoryStoreOrderRepository orderStore;
    private final InMemoryStoreAccountRepository accountStore;
    private final InMemoryStoreSalesRepository salesStore;

    public InMemoryStoreRecordRepository() {
        this(new InMemoryStoreState());
    }

    private InMemoryStoreRecordRepository(InMemoryStoreState state) {
        this(state, new InMemoryStoreProductRepository(state),
                new InMemoryStoreCartRepository(state), new InMemoryStoreOrderRepository(state),
                new InMemoryStoreAccountRepository(state), new InMemoryStoreSalesRepository(state));
    }

    private InMemoryStoreRecordRepository(InMemoryStoreState state,
                                           InMemoryStoreProductRepository products,
                                           InMemoryStoreCartRepository cart,
                                           InMemoryStoreOrderRepository orders,
                                           InMemoryStoreAccountRepository accounts,
                                           InMemoryStoreSalesRepository sales) {
        super(products, cart, orders, accounts, sales);
        this.state = state;
        this.productStore = products;
        this.orderStore = orders;
        this.accountStore = accounts;
        this.salesStore = sales;
    }

    public void addProduct(ProductDto product) { productStore.add(product); }

    public void addProduct(long id, String sku, String name, String category,
                           BigDecimal price, int stockQty, String status) {
        addProduct(new ProductDto(id, sku, name, category, null, price, stockQty, status));
    }

    public void addAccount(AccountDto account) { accountStore.add(account); }
    public void addAccount(long userId, BigDecimal balance) {
        addAccount(new AccountDto(0L, userId, balance, "ACTIVE"));
    }
    public void addTransaction(AccountTransactionDto transaction) { accountStore.addTransaction(transaction); }
    public void addOrder(OrderDto order) { orderStore.addOrder(order); }

    public ProductDto getProduct(long productId) {
        return findProduct(null, productId, false);
    }

    public OrderDto getOrder(long orderId) {
        return findOrder(null, orderId, false);
    }

    public AccountDto getAccount(long userId) { return accountStore.account(userId); }

    public int getStock(long productId) {
        ProductDto product = getProduct(productId);
        return product == null ? -1 : product.getStockQty();
    }

    public Object transactionLock() { return state; }

    public Object snapshotState() {
        synchronized (state) { return state.snapshot(); }
    }

    public void restoreState(Object snapshot) {
        if (!(snapshot instanceof InMemoryStoreState.Snapshot)) {
            throw new IllegalArgumentException("invalid store snapshot");
        }
        synchronized (state) { state.restore((InMemoryStoreState.Snapshot) snapshot); }
    }

}
