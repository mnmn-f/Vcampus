package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.ProductReviewQuery;
import edu.seu.vcampus.common.dto.store.ReviewCandidateDto;
import edu.seu.vcampus.common.dto.store.ReviewCandidatePage;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class MySqlReviewCandidates {
    private MySqlReviewCandidates() { }
    static ReviewCandidatePage load(Connection c, long user, ProductReviewQuery query) {
        ProductReviewQuery q = query == null ? new ProductReviewQuery(0) : query;
        String from = " FROM store_order_items i JOIN store_orders o ON o.id=i.order_id WHERE o.buyer_id=? AND o.status='COMPLETED'"
                + " AND NOT EXISTS (SELECT 1 FROM store_product_reviews r WHERE r.order_id=o.id AND r.product_id=i.product_id)"
                + (q.getProductId() > 0 ? " AND i.product_id=?" : "")
                + (q.getKeyword() != null ? " AND (i.product_name_snapshot LIKE ? OR o.order_no LIKE ?)" : "");
        try {
            long total;
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*)" + from)) {
                bind(ps, user, q); try (ResultSet rs = ps.executeQuery()) { rs.next(); total = rs.getLong(1); }
            }
            List<ReviewCandidateDto> rows = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT o.id,o.order_no,i.product_id,i.product_name_snapshot AS product_name,i.quantity" + from + " ORDER BY o.id DESC,i.id DESC LIMIT ? OFFSET ?")) {
                int index = bind(ps, user, q); ps.setInt(index++, q.getPageSize()); ps.setInt(index, q.getOffset());
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(new ReviewCandidateDto(rs.getLong("id"), rs.getLong("product_id"), rs.getString("order_no"), rs.getString("product_name"), rs.getInt("quantity"))); }
            }
            return new ReviewCandidatePage(rows, total);
        } catch (SQLException error) { throw new StoreRepositoryException("查询待评价商品失败", error); }
    }
    private static int bind(PreparedStatement ps, long user, ProductReviewQuery q) throws SQLException {
        int index = 1; ps.setLong(index++, user);
        if (q.getProductId() > 0) ps.setLong(index++, q.getProductId());
        if (q.getKeyword() != null) { ps.setString(index++, "%" + q.getKeyword() + "%"); ps.setString(index++, "%" + q.getKeyword() + "%"); }
        return index;
    }
}
