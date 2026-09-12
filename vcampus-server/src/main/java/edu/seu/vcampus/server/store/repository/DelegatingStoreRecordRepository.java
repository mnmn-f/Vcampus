package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.OrderDto;
import edu.seu.vcampus.common.dto.store.OrderPage;
import edu.seu.vcampus.common.dto.store.OrderQuery;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

/** 将商品、购物车、订单和账户 DAO 组合为一个事务仓储。 */
public class DelegatingStoreRecordRepository implements StoreRecordRepository {
    private final StoreProductRepository products;
    private final StoreCartRepository cart;
    private final StoreOrderRepository orders;
    private final StoreAccountRepository accounts;
    private final StoreSalesRepository sales;

    public DelegatingStoreRecordRepository(StoreProductRepository products,
                                           StoreCartRepository cart,
                                           StoreOrderRepository orders,
                                           StoreAccountRepository accounts) {
        this(products, cart, orders, accounts,
                orders instanceof StoreSalesRepository ? (StoreSalesRepository) orders : null);
    }

    public DelegatingStoreRecordRepository(StoreProductRepository products,
                                           StoreCartRepository cart,
                                           StoreOrderRepository orders,
                                           StoreAccountRepository accounts,
                                           StoreSalesRepository sales) {
        if (products == null || cart == null || orders == null || accounts == null) {
            throw new IllegalArgumentException("store repositories are required");
        }
        this.products = products;
        this.cart = cart;
        this.orders = orders;
        this.accounts = accounts;
        this.sales = sales;
    }

    @Override public ProductPage searchProducts(Connection c, ProductQuery q) {
        return products.searchProducts(c, q);
    }
    @Override public byte[] findProductImage(Connection c, String reference, boolean manager) {
        return products.findProductImage(c, reference, manager);
    }
    @Override public ProductDto findProduct(Connection c, long id, boolean lock) {
        return products.findProduct(c, id, lock);
    }
    @Override public boolean skuExists(Connection c, String sku, long id) {
        return products.skuExists(c, sku, id);
    }
    @Override public void insertProduct(Connection c, ProductWriteRequest r, long actor) {
        products.insertProduct(c, r, actor);
    }
    @Override public void updateProduct(Connection c, ProductWriteRequest r, long actor) {
        products.updateProduct(c, r, actor);
    }
    @Override public boolean adjustStock(Connection c, long id, int delta) {
        return products.adjustStock(c, id, delta);
    }
    @Override public void updateRating(Connection c, long id, BigDecimal average, long count) {
        products.updateRating(c, id, average, count);
    }

    @Override public CartDto findCart(Connection c, long userId) { return cart.findCart(c, userId); }
    @Override public void ensureCart(Connection c, long userId) { cart.ensureCart(c, userId); }
    @Override public void addCartItem(Connection c, long u, CartItemRequest r) {
        cart.addCartItem(c, u, r);
    }
    @Override public void updateCartItem(Connection c, long u, CartItemRequest r) {
        cart.updateCartItem(c, u, r);
    }
    @Override public void removeCartItem(Connection c, long u, long p) {
        cart.removeCartItem(c, u, p);
    }
    @Override public List<CartLine> findCartLines(Connection c, long u, boolean lock) {
        return cart.findCartLines(c, u, lock);
    }
    @Override public void clearCart(Connection c, long u) { cart.clearCart(c, u); }

    @Override public long insertOrder(Connection c, long u, String n, BigDecimal a) {
        return orders.insertOrder(c, u, n, a);
    }
    @Override public void insertOrderItems(Connection c, long id, List<CartLine> l) {
        orders.insertOrderItems(c, id, l);
    }
    @Override public void updateOrderPricing(Connection c, long id, BigDecimal original,
                                              BigDecimal discount, String promotion,
                                              String coupon, String mode) {
        orders.updateOrderPricing(c, id, original, discount, promotion, coupon, mode);
    }
    @Override public OrderDto findOrder(Connection c, long id, boolean lock) {
        return orders.findOrder(c, id, lock);
    }
    @Override public OrderPage findOrders(Connection c, Long u, OrderQuery q) {
        return orders.findOrders(c, u, q);
    }
    @Override public boolean decrementStock(Connection c, long p, int q) {
        return orders.decrementStock(c, p, q);
    }
    @Override public void incrementStock(Connection c, long p, int q) {
        orders.incrementStock(c, p, q);
    }
    @Override public boolean updateOrderStatus(Connection c, long id, String s) {
        return orders.updateOrderStatus(c, id, s);
    }
    @Override public boolean updateOrderShipping(Connection c, long id, String status,
            String tracking, String remark) {
        return orders.updateOrderShipping(c, id, status, tracking, remark);
    }

    @Override public StoreSalesPage findSales(Connection c, StoreSalesQuery q) {
        if (sales == null) throw new UnsupportedOperationException("sales repository is required");
        return sales.findSales(c, q);
    }

    @Override public AccountDto findAccount(Connection c, long u, boolean lock) {
        return accounts.findAccount(c, u, lock);
    }
    @Override public AccountLedgerPage findLedger(Connection c, long u, AccountLedgerQuery q) {
        return accounts.findLedger(c, u, q);
    }
    @Override public LedgerRecord findTransactionByKey(Connection c, String k) {
        return accounts.findTransactionByKey(c, k);
    }
    @Override public AccountDto findOrderPaymentAccount(Connection c, long orderId,
                                                         boolean lock) {
        return accounts.findOrderPaymentAccount(c, orderId, lock);
    }
    @Override public boolean updateAccountBalance(Connection c, long id, BigDecimal e,
                                                   BigDecimal n) {
        return accounts.updateAccountBalance(c, id, e, n);
    }
    @Override public long insertTransaction(Connection c, long id, String t, BigDecimal a,
                                             BigDecimal b, BigDecimal after, String rt,
                                             Long rid, String key, long op, String remark) {
        return accounts.insertTransaction(c, id, t, a, b, after, rt, rid, key, op, remark);
    }
}
