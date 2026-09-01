package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.AccountTransactionDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.ProductDto;

import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 内存仓储共享状态；测试事务执行器以该对象作为锁。 */
final class InMemoryStoreState {
    final Map<Long, ProductDto> products = new LinkedHashMap<Long, ProductDto>();
    final Map<Long, MemoryCart> carts = new LinkedHashMap<Long, MemoryCart>();
    final Map<Long, MemoryOrder> orders = new LinkedHashMap<Long, MemoryOrder>();
    final Map<Long, MemoryAccount> accounts = new LinkedHashMap<Long, MemoryAccount>();
    final Map<Long, List<AccountTransactionDto>> ledgers =
            new LinkedHashMap<Long, List<AccountTransactionDto>>();
    final Map<String, LedgerRecord> keys = new LinkedHashMap<String, LedgerRecord>();
    long productSequence = 1L;
    long cartSequence = 1L;
    long orderSequence = 1L;
    long accountSequence = 1L;
    long transactionSequence = 1L;

    static ProductDto withStock(ProductDto old, int stock) {
        return new ProductDto(old.getId(), old.getSku(), old.getName(), old.getCategory(),
                old.getDescription(), old.getPrice(), stock, old.getStatus(), old.getImageUrl(),
                old.getRatingAverage(), old.getRatingCount(), old.getCreatedBy(),
                old.getCreatedAt(), LocalDateTime.now());
    }

    static ProductDto withRating(ProductDto old, BigDecimal average, long count) {
        return new ProductDto(old.getId(), old.getSku(), old.getName(), old.getCategory(),
                old.getDescription(), old.getPrice(), old.getStockQty(), old.getStatus(),
                old.getImageUrl(), average, count, old.getCreatedBy(), old.getCreatedAt(),
                LocalDateTime.now());
    }

    static final class MemoryCart {
        final long id;
        final long userId;
        String status = "ACTIVE";
        final Map<Long, Integer> quantities = new LinkedHashMap<Long, Integer>();

        MemoryCart(long id, long userId) {
            this.id = id;
            this.userId = userId;
        }
    }

    static final class MemoryOrder {
        final long id;
        final String orderNo;
        final long buyerId;
        final BigDecimal totalAmount;
        BigDecimal originalAmount;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String promotionCode;
        String couponCode;
        String paymentMode = "SELF";
        final LocalDateTime createdAt;
        String status = "CREATED";
        LocalDateTime paidAt;
        LocalDateTime cancelledAt;
        LocalDateTime completedAt;
        final List<OrderItemDto> items = new ArrayList<OrderItemDto>();

        MemoryOrder(long id, String orderNo, long buyerId, BigDecimal totalAmount) {
            this(id, orderNo, buyerId, totalAmount, LocalDateTime.now());
        }

        MemoryOrder(long id, String orderNo, long buyerId, BigDecimal totalAmount,
                    LocalDateTime createdAt) {
            this.id = id;
            this.orderNo = orderNo;
            this.buyerId = buyerId;
            this.totalAmount = totalAmount;
            this.originalAmount = totalAmount;
            this.createdAt = createdAt;
        }
    }

    static final class MemoryAccount {
        final long id;
        final long userId;
        BigDecimal balance;
        String status;

        MemoryAccount(long id, long userId, BigDecimal balance, String status) {
            this.id = id;
            this.userId = userId;
            this.balance = balance;
            this.status = status;
        }
    }

    Snapshot snapshot() {
        Snapshot result = new Snapshot();
        result.state.products.putAll(products);
        for (Map.Entry<Long, MemoryCart> entry : carts.entrySet()) {
            MemoryCart copy = new MemoryCart(entry.getValue().id, entry.getValue().userId);
            copy.status = entry.getValue().status;
            copy.quantities.putAll(entry.getValue().quantities);
            result.state.carts.put(entry.getKey(), copy);
        }
        for (Map.Entry<Long, MemoryOrder> entry : orders.entrySet()) {
            MemoryOrder value = entry.getValue();
            MemoryOrder copy = new MemoryOrder(value.id, value.orderNo, value.buyerId,
                    value.totalAmount, value.createdAt);
            copy.status = value.status;
            copy.originalAmount = value.originalAmount;
            copy.discountAmount = value.discountAmount;
            copy.promotionCode = value.promotionCode;
            copy.couponCode = value.couponCode;
            copy.paymentMode = value.paymentMode;
            copy.paidAt = value.paidAt;
            copy.cancelledAt = value.cancelledAt;
            copy.completedAt = value.completedAt;
            copy.items.addAll(value.items);
            result.state.orders.put(entry.getKey(), copy);
        }
        for (Map.Entry<Long, MemoryAccount> entry : accounts.entrySet()) {
            MemoryAccount value = entry.getValue();
            result.state.accounts.put(entry.getKey(), new MemoryAccount(value.id, value.userId,
                    value.balance, value.status));
        }
        for (Map.Entry<Long, List<AccountTransactionDto>> entry : ledgers.entrySet()) {
            result.state.ledgers.put(entry.getKey(), new ArrayList<AccountTransactionDto>(entry.getValue()));
        }
        result.state.keys.putAll(keys);
        result.state.productSequence = productSequence;
        result.state.cartSequence = cartSequence;
        result.state.orderSequence = orderSequence;
        result.state.accountSequence = accountSequence;
        result.state.transactionSequence = transactionSequence;
        return result;
    }

    void restore(Snapshot snapshot) {
        products.clear();
        products.putAll(snapshot.state.products);
        carts.clear();
        carts.putAll(snapshot.state.carts);
        orders.clear();
        orders.putAll(snapshot.state.orders);
        accounts.clear();
        accounts.putAll(snapshot.state.accounts);
        ledgers.clear();
        ledgers.putAll(snapshot.state.ledgers);
        keys.clear();
        keys.putAll(snapshot.state.keys);
        productSequence = snapshot.state.productSequence;
        cartSequence = snapshot.state.cartSequence;
        orderSequence = snapshot.state.orderSequence;
        accountSequence = snapshot.state.accountSequence;
        transactionSequence = snapshot.state.transactionSequence;
    }

    static final class Snapshot {
        final InMemoryStoreState state = new InMemoryStoreState();
    }
}
