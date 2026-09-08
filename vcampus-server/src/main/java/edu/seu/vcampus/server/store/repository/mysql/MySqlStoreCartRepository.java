package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.CartDto;
import edu.seu.vcampus.common.dto.store.CartItemDto;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.server.store.repository.CartLine;
import edu.seu.vcampus.server.store.repository.StoreCartRepository;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** shopping_carts 与 cart_items 的 MySQL DAO。 */
public final class MySqlStoreCartRepository implements StoreCartRepository {
    private static final String LINE_COLUMNS = "p.id, p.sku, p.name, p.price, ci.quantity, "
            + "p.stock_qty, p.status";

    @Override
    public CartDto findCart(Connection c, long userId) {
        String sql = "SELECT id, status FROM shopping_carts WHERE user_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    List<CartLine> lines = findCartLines(c, userId, false);
                    return toCart(rs.getLong("id"), userId, rs.getString("status"), lines);
                }
                return new CartDto(0L, userId, "ACTIVE", new ArrayList<CartItemDto>(),
                        BigDecimal.ZERO);
            }
        } catch (SQLException ex) {
            throw fail("查询购物车失败", ex);
        }
    }

    @Override
    public void ensureCart(Connection c, long userId) {
        String sql = "INSERT INTO shopping_carts (user_id, status) VALUES (?, 'ACTIVE') "
                + "ON DUPLICATE KEY UPDATE status = 'ACTIVE'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw fail("创建购物车失败", ex);
        }
    }

    @Override
    public void addCartItem(Connection c, long userId, CartItemRequest request) {
        String sql = "INSERT INTO cart_items (cart_id, product_id, quantity) "
                + "SELECT id, ?, ? FROM shopping_carts WHERE user_id = ? AND status = 'ACTIVE' "
                + "ON DUPLICATE KEY UPDATE quantity = quantity + ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, request.getProductId());
            ps.setInt(2, request.getQuantity());
            ps.setLong(3, userId);
            ps.setInt(4, request.getQuantity());
            if (ps.executeUpdate() < 1) throw new StoreRepositoryException("购物车不可用");
        } catch (SQLException ex) {
            throw fail("加入购物车失败", ex);
        }
    }

    @Override
    public void updateCartItem(Connection c, long userId, CartItemRequest request) {
        String sql = "UPDATE cart_items ci JOIN shopping_carts sc ON sc.id = ci.cart_id "
                + "SET ci.quantity = ? WHERE sc.user_id = ? AND sc.status = 'ACTIVE' "
                + "AND ci.product_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, request.getQuantity());
            ps.setLong(2, userId);
            ps.setLong(3, request.getProductId());
            if (ps.executeUpdate() != 1) throw new StoreRepositoryException("购物车商品不存在");
        } catch (SQLException ex) {
            throw fail("修改购物车失败", ex);
        }
    }

    @Override
    public void removeCartItem(Connection c, long userId, long productId) {
        String sql = "DELETE ci FROM cart_items ci JOIN shopping_carts sc ON sc.id = ci.cart_id "
                + "WHERE sc.user_id = ? AND sc.status = 'ACTIVE' AND ci.product_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw fail("移除购物车商品失败", ex);
        }
    }

    @Override
    public List<CartLine> findCartLines(Connection c, long userId, boolean forUpdate) {
        String sql = "SELECT " + LINE_COLUMNS + " FROM shopping_carts sc "
                + "JOIN cart_items ci ON ci.cart_id = sc.id JOIN products p ON p.id = ci.product_id "
                + "WHERE sc.user_id = ? AND sc.status = 'ACTIVE' ORDER BY p.id"
                + (forUpdate ? " FOR UPDATE" : "");
        List<CartLine> lines = new ArrayList<CartLine>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lines.add(new CartLine(rs.getLong("id"), rs.getString("sku"),
                        rs.getString("name"), rs.getBigDecimal("price"), rs.getInt("quantity"),
                        rs.getInt("stock_qty"), rs.getString("status")));
            }
            return lines;
        } catch (SQLException ex) {
            throw fail("读取购物车商品失败", ex);
        }
    }

    @Override
    public void clearCart(Connection c, long userId) {
        String sql = "DELETE ci FROM cart_items ci JOIN shopping_carts sc ON sc.id = ci.cart_id "
                + "WHERE sc.user_id = ? AND sc.status = 'ACTIVE'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw fail("清空购物车失败", ex);
        }
    }

    private static CartDto toCart(long id, long userId, String status, List<CartLine> lines) {
        List<CartItemDto> items = new ArrayList<CartItemDto>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : lines) {
            items.add(line.toCartItem());
            total = total.add(line.lineAmount());
        }
        return new CartDto(id, userId, status, items, total);
    }

    private static StoreRepositoryException fail(String message, Throwable cause) {
        return new StoreRepositoryException(message, cause);
    }
}
