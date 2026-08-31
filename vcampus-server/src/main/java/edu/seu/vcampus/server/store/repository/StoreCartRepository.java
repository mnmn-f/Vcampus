package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;

import java.sql.Connection;
import java.util.List;

/** shopping_carts/cart_items 表的读写边界。 */
public interface StoreCartRepository {
    CartDto findCart(Connection connection, long userId);
    void ensureCart(Connection connection, long userId);
    void addCartItem(Connection connection, long userId, CartItemRequest request);
    void updateCartItem(Connection connection, long userId, CartItemRequest request);
    void removeCartItem(Connection connection, long userId, long productId);
    List<CartLine> findCartLines(Connection connection, long userId, boolean forUpdate);
    void clearCart(Connection connection, long userId);
}
