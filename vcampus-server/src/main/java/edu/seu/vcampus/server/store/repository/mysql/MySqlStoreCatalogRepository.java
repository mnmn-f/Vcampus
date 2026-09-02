package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.PromotionDto;
import edu.seu.vcampus.common.dto.store.PromotionPage;
import edu.seu.vcampus.common.dto.store.PromotionWriteRequest;
import edu.seu.vcampus.common.dto.store.StoreCategoryDto;
import edu.seu.vcampus.common.dto.store.StoreCategoryPage;
import edu.seu.vcampus.common.dto.store.StoreCategoryWriteRequest;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** V4 分类与促销规则的 MySQL DAO。 */
final class MySqlStoreCatalogRepository {
    StoreCategoryPage categories(Connection c, boolean includeInactive) {
        String sql = "SELECT id, code, name, active FROM store_categories"
                + (includeInactive ? "" : " WHERE active = 1") + " ORDER BY name, id";
        List<StoreCategoryDto> rows = new ArrayList<StoreCategoryDto>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(new StoreCategoryDto(rs.getLong("id"),
                    rs.getString("code"), rs.getString("name"), rs.getBoolean("active")));
            return new StoreCategoryPage(rows, rows.size());
        } catch (SQLException ex) { throw fail("查询商品分类失败", ex); }
    }

    StoreCategoryDto saveCategory(Connection c, StoreCategoryWriteRequest r) {
        try {
            if (r.getId() > 0L) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE store_categories SET code=?, name=?, active=? WHERE id=?")) {
                    ps.setString(1, r.getCode()); ps.setString(2, r.getName());
                    ps.setBoolean(3, r.isActive()); ps.setLong(4, r.getId());
                    if (ps.executeUpdate() != 1) throw new StoreRepositoryException("分类不存在");
                }
                return category(c, r.getId());
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO store_categories(code,name,active) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, r.getCode()); ps.setString(2, r.getName()); ps.setBoolean(3, r.isActive());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new StoreRepositoryException("分类编号生成失败");
                    return category(c, keys.getLong(1));
                }
            }
        } catch (SQLException ex) { throw fail("保存商品分类失败", ex); }
    }

    PromotionPage promotions(Connection c) {
        List<PromotionDto> rows = new ArrayList<PromotionDto>();
        String sql = "SELECT id,code,name,promotion_type,threshold_amount,discount_value,"
                + "product_scope,product_id,category_code,starts_at,ends_at,stackable,active"
                + " FROM store_promotions ORDER BY starts_at DESC,id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(readPromotion(rs));
            return new PromotionPage(rows, rows.size());
        } catch (SQLException ex) { throw fail("查询促销规则失败", ex); }
    }

    List<PromotionDto> activePromotions(Connection c) {
        List<PromotionDto> rows = new ArrayList<PromotionDto>();
        String sql = "SELECT id,code,name,promotion_type,threshold_amount,discount_value,"
                + "product_scope,product_id,category_code,starts_at,ends_at,stackable,active"
                + " FROM store_promotions WHERE active=1 AND starts_at<=CURRENT_TIMESTAMP(3)"
                + " AND (ends_at IS NULL OR ends_at>CURRENT_TIMESTAMP(3)) ORDER BY id";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(readPromotion(rs));
            return rows;
        } catch (SQLException ex) { throw fail("查询生效促销失败", ex); }
    }

    PromotionDto savePromotion(Connection c, PromotionWriteRequest r) {
        String sql;
        try {
            if (r.getId() > 0L) {
                sql = "UPDATE store_promotions SET code=?,name=?,promotion_type=?,threshold_amount=?,"
                        + "discount_value=?,product_scope=?,product_id=?,category_code=?,starts_at=?,ends_at=?,stackable=?,active=? WHERE id=?";
            } else {
                sql = "INSERT INTO store_promotions(code,name,promotion_type,threshold_amount,discount_value,"
                        + "product_scope,product_id,category_code,starts_at,ends_at,stackable,active) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
            }
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = bindPromotion(ps, r); if (r.getId() > 0L) ps.setLong(i, r.getId());
                ps.executeUpdate();
                long id = r.getId();
                if (id == 0L) try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new StoreRepositoryException("促销编号生成失败"); id = keys.getLong(1); }
                return promotion(c, id);
            }
        } catch (SQLException ex) { throw fail("保存促销规则失败", ex); }
    }

    private static int bindPromotion(PreparedStatement ps, PromotionWriteRequest r) throws SQLException {
        ps.setString(1, r.getCode()); ps.setString(2, r.getName()); ps.setString(3, r.getType());
        set(ps, 4, r.getThreshold()); set(ps, 5, r.getValue()); ps.setString(6, r.getProductScope());
        if (r.getProductId() == null) ps.setNull(7, java.sql.Types.BIGINT); else ps.setLong(7, r.getProductId());
        ps.setString(8, r.getCategoryCode()); ps.setTimestamp(9, JdbcTemporal.timestamp(r.getStartsAt()));
        ps.setTimestamp(10, JdbcTemporal.timestamp(r.getEndsAt())); ps.setBoolean(11, r.isStackable()); ps.setBoolean(12, r.isActive());
        return 13;
    }

    private static PromotionDto readPromotion(ResultSet rs) throws SQLException {
        Long product = rs.getLong("product_id"); if (rs.wasNull()) product = null;
        return new PromotionDto(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("promotion_type"), rs.getBigDecimal("threshold_amount"), rs.getBigDecimal("discount_value"),
                rs.getString("product_scope"), product, rs.getString("category_code"),
                JdbcTemporal.localDateTime(rs.getTimestamp("starts_at")), JdbcTemporal.localDateTime(rs.getTimestamp("ends_at")),
                rs.getBoolean("stackable"), rs.getBoolean("active"));
    }
    private StoreCategoryDto category(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id,code,name,active FROM store_categories WHERE id=?")) {
            ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new StoreRepositoryException("分类不存在"); return new StoreCategoryDto(id, rs.getString("code"), rs.getString("name"), rs.getBoolean("active")); }
        }
    }
    private PromotionDto promotion(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id,code,name,promotion_type,threshold_amount,discount_value,product_scope,product_id,category_code,starts_at,ends_at,stackable,active FROM store_promotions WHERE id=?")) {
            ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new StoreRepositoryException("促销不存在"); return readPromotion(rs); }
        }
    }
    private static void set(PreparedStatement ps, int i, java.math.BigDecimal v) throws SQLException { if (v == null) ps.setNull(i, java.sql.Types.DECIMAL); else ps.setBigDecimal(i, v); }
    private static StoreRepositoryException fail(String m, Throwable x) { return new StoreRepositoryException(m, x); }
}
