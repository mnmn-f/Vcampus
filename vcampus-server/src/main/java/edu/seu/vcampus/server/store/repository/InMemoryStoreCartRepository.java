package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.store.ProductDto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 购物车内存仓储；所有变更在共享状态锁内完成。 */
final class InMemoryStoreCartRepository implements StoreCartRepository {
    private final InMemoryStoreState state;

    InMemoryStoreCartRepository(InMemoryStoreState state) { this.state = state; }

    @Override
    public CartDto findCart(Connection c, long userId) {
        synchronized (state) {
            InMemoryStoreState.MemoryCart cart = state.carts.get(userId);
            return cart == null ? new CartDto(0L, userId, "ACTIVE",
                    Collections.<CartItemDto>emptyList(), BigDecimal.ZERO) : toDto(cart);
        }
    }

    @Override
    public void ensureCart(Connection c, long userId) {
        synchronized (state) { cart(userId); }
    }

    @Override
    public void addCartItem(Connection c, long userId, CartItemRequest request) {
        synchronized (state) {
            InMemoryStoreState.MemoryCart cart = cart(userId);
            int old = value(cart.quantities, request.getProductId());
            cart.quantities.put(request.getProductId(), old + request.getQuantity());
        }
    }

    @Override
    public void updateCartItem(Connection c, long userId, CartItemRequest request) {
        synchronized (state) {
            InMemoryStoreState.MemoryCart cart = cart(userId);
            if (!cart.quantities.containsKey(request.getProductId())) {
                throw new StoreRepositoryException("购物车商品不存在");
            }
            cart.quantities.put(request.getProductId(), request.getQuantity());
        }
    }

    @Override
    public void removeCartItem(Connection c, long userId, long productId) {
        synchronized (state) {
            InMemoryStoreState.MemoryCart cart = state.carts.get(userId);
            if (cart != null) cart.quantities.remove(productId);
        }
    }

    @Override
    public List<CartLine> findCartLines(Connection c, long userId, boolean forUpdate) {
        synchronized (state) { return lines(state.carts.get(userId)); }
    }

    @Override
    public void clearCart(Connection c, long userId) {
        synchronized (state) {
            InMemoryStoreState.MemoryCart cart = state.carts.get(userId);
            if (cart != null) cart.quantities.clear();
        }
    }

    private InMemoryStoreState.MemoryCart cart(long userId) {
        InMemoryStoreState.MemoryCart found = state.carts.get(userId);
        if (found == null) {
            found = new InMemoryStoreState.MemoryCart(state.cartSequence++, userId);
            state.carts.put(userId, found);
        }
        found.status = "ACTIVE";
        return found;
    }

    private List<CartLine> lines(InMemoryStoreState.MemoryCart cart) {
        List<CartLine> result = new ArrayList<CartLine>();
        if (cart == null) return result;
        List<Long> ids = new ArrayList<Long>(cart.quantities.keySet());
        Collections.sort(ids);
        for (Long id : ids) {
            ProductDto product = state.products.get(id);
            if (product != null) result.add(new CartLine(product.getId(), product.getSku(),
                    product.getName(), product.getPrice(), cart.quantities.get(id),
                    product.getStockQty(), product.getStatus()));
        }
        return result;
    }

    private CartDto toDto(InMemoryStoreState.MemoryCart cart) {
        List<CartLine> lines = lines(cart);
        List<CartItemDto> items = new ArrayList<CartItemDto>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : lines) {
            items.add(line.toCartItem());
            total = total.add(line.lineAmount());
        }
        return new CartDto(cart.id, cart.userId, cart.status, items, total);
    }

    private static int value(Map<Long, Integer> values, long key) {
        Integer value = values.get(key);
        return value == null ? 0 : value.intValue();
    }
}
