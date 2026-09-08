package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountTransactionDto;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendDto;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.threeten.bp.LocalDate;

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

    public List<StoreSalesTrendDto> findTrend(StoreSalesTrendQuery query) {
        StoreSalesTrendQuery safe = query == null ? new StoreSalesTrendQuery() : query;
        Map<LocalDate, TrendTotal> totals = new TreeMap<LocalDate, TrendTotal>();
        synchronized (state) {
            for (InMemoryStoreState.MemoryOrder order : state.orders.values()) {
                if (!("PAID".equals(order.status) || "COMPLETED".equals(order.status))
                        || order.paidAt == null) continue;
                LocalDate date = order.paidAt.toLocalDate();
                if (safe.getStartDate() != null && date.isBefore(safe.getStartDate())) continue;
                if (safe.getEndDate() != null && date.isAfter(safe.getEndDate())) continue;
                TrendTotal total = totals.get(date);
                if (total == null) { total = new TrendTotal(); totals.put(date, total); }
                total.amount = total.amount.add(order.totalAmount);
                for (edu.seu.vcampus.common.dto.store.OrderItemDto item : order.items) {
                    total.quantity += item.getQuantity();
                }
            }
        }
        List<StoreSalesTrendDto> rows = new ArrayList<StoreSalesTrendDto>();
        for (Map.Entry<LocalDate, TrendTotal> entry : totals.entrySet()) {
            rows.add(new StoreSalesTrendDto(entry.getKey(), entry.getValue().quantity,
                    entry.getValue().amount));
        }
        return rows;
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

    private static final class TrendTotal {
        private long quantity;
        private BigDecimal amount = BigDecimal.ZERO;
    }

}
