package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import edu.seu.vcampus.common.dto.store.ProductReviewDto;
import edu.seu.vcampus.common.dto.store.ProductReviewPage;
import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ProductReviewWriteRequest;
import edu.seu.vcampus.server.db.JdbcTemporal;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** V4 优惠券与商品评价的 MySQL DAO。 */
final class MySqlStoreCouponReviewRepository {
    CouponPage coupons(Connection c, long userId) {
        String sql = "SELECT sc.id,sc.code,sc.name,sc.threshold_amount,sc.discount_amount,sc.expires_at,"
                + "suc.id IS NOT NULL AS claimed,suc.used_at IS NOT NULL AS used FROM store_coupons sc "
                + "LEFT JOIN store_user_coupons suc ON suc.coupon_id=sc.id AND suc.user_id=? "
                + "WHERE sc.active=1 ORDER BY sc.expires_at,sc.id";
        List<CouponDto> rows = new ArrayList<CouponDto>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(readCoupon(rs)); }
            return new CouponPage(rows, rows.size());
        } catch (SQLException ex) { throw fail("查询优惠券失败", ex); }
    }

    CouponDto claim(Connection c, long userId, CouponClaimRequest request) {
        try {
            long couponId;
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM store_coupons WHERE code=? AND active=1 AND expires_at>CURRENT_TIMESTAMP(3) FOR UPDATE")) {
                ps.setString(1, request.getCode()); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new StoreRepositoryException("优惠券不存在或已过期"); couponId = rs.getLong(1); }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO store_user_coupons(coupon_id,user_id) VALUES(?,?)")) {
                ps.setLong(1, couponId); ps.setLong(2, userId); ps.executeUpdate();
            }
            return coupon(c, userId, request.getCode(), false);
        } catch (SQLException ex) { throw fail("领取优惠券失败，可能已领取", ex); }
    }

    CouponDto findCoupon(Connection c, long userId, String code, boolean lock) {
        String sql = "SELECT sc.id,sc.code,sc.name,sc.threshold_amount,sc.discount_amount,sc.expires_at,"
                + "1 AS claimed,suc.used_at IS NOT NULL AS used FROM store_coupons sc JOIN store_user_coupons suc "
                + "ON suc.coupon_id=sc.id AND suc.user_id=? WHERE sc.code=? AND sc.active=1" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId); ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? readCoupon(rs) : null; }
        } catch (SQLException ex) { throw fail("查询用户优惠券失败", ex); }
    }

    boolean markCouponUsed(Connection c, long userId, String code, long orderId) {
        String sql = "UPDATE store_user_coupons suc JOIN store_coupons sc ON sc.id=suc.coupon_id "
                + "SET suc.used_at=CURRENT_TIMESTAMP(3),suc.order_id=? WHERE suc.user_id=? AND sc.code=? AND suc.used_at IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.setLong(1, orderId); ps.setLong(2, userId); ps.setString(3, code); return ps.executeUpdate() == 1; }
        catch (SQLException ex) { throw fail("使用优惠券失败", ex); }
    }

    ProductReviewPage reviews(Connection c, ProductReviewQuery query) {
        ProductReviewQuery q = query == null ? new ProductReviewQuery(0L) : query;
        String where = q.getProductId() > 0L ? " WHERE r.product_id=?" : "";
        String sql = "SELECT r.id,r.product_id,r.order_id,p.name,r.score,r.content,r.created_at FROM store_product_reviews r "
                + "JOIN products p ON p.id=r.product_id" + where + " ORDER BY r.created_at DESC,r.id DESC LIMIT ? OFFSET ?";
        List<ProductReviewDto> rows = new ArrayList<ProductReviewDto>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1; if (q.getProductId() > 0L) ps.setLong(i++, q.getProductId()); ps.setInt(i++, q.getPageSize()); ps.setInt(i, q.getOffset());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(readReview(rs)); }
            return new ProductReviewPage(rows, countReviews(c, q));
        } catch (SQLException ex) { throw fail("查询商品评价失败", ex); }
    }

    ProductReviewDto addReview(Connection c, long userId, ProductReviewWriteRequest r, String reviewerName) {
        String sql = "INSERT INTO store_product_reviews(product_id,order_id,user_id,score,content) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, r.getProductId()); ps.setLong(2, r.getOrderId()); ps.setLong(3, userId); ps.setInt(4, r.getScore());
            if (r.getContent() == null) ps.setNull(5, java.sql.Types.VARCHAR); else ps.setString(5, r.getContent()); ps.executeUpdate();
            try (PreparedStatement update = c.prepareStatement("UPDATE products SET rating_average=(SELECT COALESCE(AVG(score),0) FROM store_product_reviews WHERE product_id=?),rating_count=(SELECT COUNT(*) FROM store_product_reviews WHERE product_id=?) WHERE id=?")) {
                update.setLong(1, r.getProductId()); update.setLong(2, r.getProductId()); update.setLong(3, r.getProductId()); update.executeUpdate();
            }
            try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new StoreRepositoryException("评价编号生成失败"); return review(c, keys.getLong(1), reviewerName); }
        } catch (SQLException ex) { throw fail("提交评价失败，订单明细可能已评价", ex); }
    }

    private static CouponDto readCoupon(ResultSet rs) throws SQLException {
        return new CouponDto(rs.getLong("id"), rs.getString("code"), rs.getString("name"), rs.getBigDecimal("threshold_amount"),
                rs.getBigDecimal("discount_amount"), JdbcTemporal.localDateTime(rs.getTimestamp("expires_at")),
                rs.getBoolean("claimed"), rs.getBoolean("used"));
    }
    private CouponDto coupon(Connection c, long user, String code, boolean lock) throws SQLException {
        CouponDto result = findCoupon(c, user, code, lock); if (result == null) throw new StoreRepositoryException("优惠券领取失败"); return result;
    }
    private static ProductReviewDto readReview(ResultSet rs) throws SQLException {
        return new ProductReviewDto(rs.getLong("id"), rs.getLong("product_id"), rs.getLong("order_id"), rs.getString("name"),
                rs.getInt("score"), rs.getString("content"), "匿名用户", JdbcTemporal.localDateTime(rs.getTimestamp("created_at")));
    }
    private ProductReviewDto review(Connection c, long id, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT r.id,r.product_id,r.order_id,p.name,r.score,r.content,r.created_at FROM store_product_reviews r JOIN products p ON p.id=r.product_id WHERE r.id=?")) {
            ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new StoreRepositoryException("评价不存在"); ProductReviewDto v = readReview(rs); return new ProductReviewDto(v.getId(),v.getProductId(),v.getOrderId(),v.getProductName(),v.getScore(),v.getContent(),name,v.getCreatedAt()); }
        }
    }
    private static long countReviews(Connection c, ProductReviewQuery q) throws SQLException {
        String sql = "SELECT COUNT(*) FROM store_product_reviews" + (q.getProductId() > 0L ? " WHERE product_id=?" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) { if (q.getProductId() > 0L) ps.setLong(1, q.getProductId()); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); } }
    }
    private static StoreRepositoryException fail(String m, Throwable x) { return new StoreRepositoryException(m, x); }
}
