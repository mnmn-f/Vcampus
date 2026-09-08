package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductQuery;
import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.server.store.repository.StoreProductRepository;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** products 表的 MySQL PreparedStatement DAO。 */
public final class MySqlStoreProductRepository implements StoreProductRepository {
    private static final String COLUMNS = "id, sku, name, COALESCE(category_code, category) AS category, description, price, "
            + "stock_qty, status, image_url, rating_average, rating_count, created_by, "
            + "created_at, updated_at";
    private static final String TABLE = " FROM products";
    @Override
    public ProductPage searchProducts(Connection c, ProductQuery query) {
        ProductQuery q = query == null ? new ProductQuery() : query;
        String where = filters(q);
        List<ProductDto> items = new ArrayList<ProductDto>();
        String sql = "SELECT " + COLUMNS + TABLE + where
                + " ORDER BY name, id LIMIT ? OFFSET ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = bindFilters(ps, q, 1);
            ps.setInt(i++, q.getPageSize());
            ps.setInt(i, q.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(read(rs));
            }
            return new ProductPage(items, q.getPage(), q.getPageSize(), count(c, q, where));
        } catch (SQLException ex) {
            throw fail("查询商品失败", ex);
        }
    }
    @Override
    public ProductDto findProduct(Connection c, long id, boolean forUpdate) {
        String sql = "SELECT " + COLUMNS + TABLE + " WHERE id = ?"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        } catch (SQLException ex) {
            throw fail("查询商品详情失败", ex);
        }
    }
    @Override
    public boolean skuExists(Connection c, String sku, long excludedId) {
        String sql = "SELECT 1 FROM products WHERE sku = ? AND id <> ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.setLong(2, excludedId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException ex) {
            throw fail("检查商品编码失败", ex);
        }
    }

    @Override
    public void insertProduct(Connection c, ProductWriteRequest r, long actor) {
        String sql = "INSERT INTO products (sku, name, category, description, price, "
                + "stock_qty, status, image_url, created_by, category_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindWrite(ps, r, actor);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw fail("新增商品失败", ex);
        }
    }

    @Override
    public void updateProduct(Connection c, ProductWriteRequest r, long actor) {
        String sql = "UPDATE products SET sku = ?, name = ?, category = ?, description = ?, "
                + "price = ?, stock_qty = ?, status = ?, image_url = ?, category_code = ?, "
                + "version = version + 1 WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bindUpdate(ps, r);
            ps.setLong(10, r.getId());
            if (ps.executeUpdate() != 1) throw new StoreRepositoryException("商品不存在");
        } catch (SQLException ex) {
            throw fail("修改商品失败", ex);
        }
    }

    @Override
    public boolean adjustStock(Connection c, long productId, int delta) {
        boolean decrease = delta < 0;
        String sql = "UPDATE products SET stock_qty = stock_qty "
                + (decrease ? "-" : "+") + " ?, version = version + 1 WHERE id = ?"
                + (decrease ? " AND stock_qty >= ?" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, decrease ? -delta : delta);
            ps.setLong(2, productId);
            if (decrease) ps.setInt(3, -delta);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw fail("调整商品库存失败", ex);
        }
    }

    @Override
    public void updateRating(Connection c, long productId, java.math.BigDecimal average, long count) {
        String sql = "UPDATE products SET rating_average=?,rating_count=? WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, average); ps.setLong(2, count); ps.setLong(3, productId);
            if (ps.executeUpdate() != 1) throw new StoreRepositoryException("商品不存在");
        } catch (SQLException ex) { throw fail("更新商品评分失败", ex); }
    }

    private long count(Connection c, ProductQuery q, String where) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*)" + TABLE + where)) {
            bindFilters(ps, q, 1);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private static String filters(ProductQuery q) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (q.getKeyword() != null) where.append(" AND (sku LIKE ? OR name LIKE ? OR description LIKE ?)");
        if (q.getCategory() != null) where.append(" AND COALESCE(category_code, category) = ?");
        if (q.getStatus() != null) where.append(" AND status = ?");
        return where.toString();
    }

    private static int bindFilters(PreparedStatement ps, ProductQuery q, int index)
            throws SQLException {
        if (q.getKeyword() != null) {
            String value = "%" + q.getKeyword() + "%";
            ps.setString(index++, value);
            ps.setString(index++, value);
            ps.setString(index++, value);
        }
        if (q.getCategory() != null) ps.setString(index++, q.getCategory());
        if (q.getStatus() != null) ps.setString(index++, q.getStatus());
        return index;
    }

    private static int bindWrite(PreparedStatement ps, ProductWriteRequest r, long actor)
            throws SQLException {
        int i = bindCommon(ps, r);
        ps.setLong(i++, actor);
        setNullable(ps, i, r.getCategory());
        return i + 1;
    }

    private static void bindUpdate(PreparedStatement ps, ProductWriteRequest r)
            throws SQLException {
        int i = bindCommon(ps, r);
        setNullable(ps, i, r.getCategory());
    }

    private static int bindCommon(PreparedStatement ps, ProductWriteRequest r)
            throws SQLException {
        ps.setString(1, r.getSku()); ps.setString(2, r.getName());
        setNullable(ps, 3, r.getCategory()); setNullable(ps, 4, r.getDescription());
        ps.setBigDecimal(5, r.getPrice()); ps.setInt(6, r.getStockQty());
        ps.setString(7, r.getStatus()); setNullable(ps, 8, r.getImageUrl());
        return 9;
    }

    private static void setNullable(PreparedStatement ps, int index, String value)
            throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.VARCHAR);
        else ps.setString(index, value);
    }

    static ProductDto read(ResultSet rs) throws SQLException {
        return new ProductDto(rs.getLong("id"), rs.getString("sku"), rs.getString("name"),
                rs.getString("category"), rs.getString("description"), rs.getBigDecimal("price"),
                rs.getInt("stock_qty"), rs.getString("status"), rs.getString("image_url"),
                rs.getBigDecimal("rating_average"), rs.getLong("rating_count"),
                nullableLong(rs, "created_by"), time(rs.getTimestamp("created_at")),
                time(rs.getTimestamp("updated_at")));
    }

    static LocalDateTime time(Timestamp timestamp) {
        return JdbcTemporal.localDateTime(timestamp);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }

    private static StoreRepositoryException fail(String message, Throwable cause) {
        return new StoreRepositoryException(message, cause);
    }
}
