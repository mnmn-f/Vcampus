package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.common.dto.store.StoreSalesDto;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.server.store.repository.StoreSalesRepository;
import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.threeten.bp.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 直接聚合 store_orders 与 store_order_items 的 MySQL 销售统计 DAO。 */
public final class MySqlStoreSalesRepository implements StoreSalesRepository {
    private static final String FROM = " FROM store_orders so JOIN store_order_items soi"
            + " ON soi.order_id = so.id LEFT JOIN products p ON p.id = soi.product_id";

    @Override
    public StoreSalesPage findSales(Connection connection, StoreSalesQuery query) {
        StoreSalesQuery q = query == null ? new StoreSalesQuery() : query;
        Filter filter = new Filter(q);
        try {
            String where = filter.where();
            long total = count(connection, where, filter.values);
            Summary summary = summary(connection, where, filter.values);
            List<StoreSalesDto> rows = rows(connection, where, filter.values, q);
            return new StoreSalesPage(rows, q.getPage(), q.getPageSize(), total,
                    summary.quantity, summary.amount);
        } catch (SQLException ex) {
            throw new StoreRepositoryException("查询销售统计失败", ex);
        }
    }

    private static long count(Connection c, String where, List<Object> values) throws SQLException {
        String sql = "SELECT COUNT(*) FROM (SELECT soi.product_id" + FROM + where
                + " GROUP BY soi.product_id) grouped_sales";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private static Summary summary(Connection c, String where, List<Object> values) throws SQLException {
        String sql = "SELECT COALESCE(SUM(soi.quantity),0), COALESCE(SUM(soi.line_amount),0)"
                + FROM + where;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Summary(rs.getLong(1), rs.getBigDecimal(2));
            }
        }
    }

    private static List<StoreSalesDto> rows(Connection c, String where, List<Object> values,
                                            StoreSalesQuery q) throws SQLException {
        String sql = "SELECT soi.product_id, MAX(p.sku) AS sku,"
                + " COALESCE(MAX(p.name),MAX(soi.product_name_snapshot)) AS product_name,"
                + " SUM(soi.quantity) AS quantity_sold, SUM(soi.line_amount) AS sales_amount"
                + FROM + where + " GROUP BY soi.product_id"
                + " ORDER BY sales_amount DESC, soi.product_id LIMIT ? OFFSET ?";
        List<StoreSalesDto> rows = new ArrayList<StoreSalesDto>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int index = bind(ps, values);
            ps.setInt(index++, q.getPageSize());
            ps.setInt(index, q.getOffset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new StoreSalesDto(rs.getLong("product_id"),
                        rs.getString("sku"), rs.getString("product_name"),
                        rs.getLong("quantity_sold"), rs.getBigDecimal("sales_amount")));
            }
        }
        return rows;
    }

    private static int bind(PreparedStatement ps, List<Object> values) throws SQLException {
        int index = 1;
        for (Object value : values) ps.setObject(index++, value);
        return index;
    }

    private static final class Filter {
        private final StoreSalesQuery query;
        private final List<Object> values = new ArrayList<Object>();

        private Filter(StoreSalesQuery query) { this.query = query; }

        private String where() {
            StringBuilder result = new StringBuilder(" WHERE so.status IN ('PAID','COMPLETED')"
                    + " AND so.paid_at IS NOT NULL");
            if (query.getStartDate() != null) {
                result.append(" AND so.paid_at >= ?");
                values.add(JdbcTemporal.timestamp(query.getStartDate().atStartOfDay()));
            }
            if (query.getEndDate() != null) {
                result.append(" AND so.paid_at < ?");
                values.add(JdbcTemporal.timestamp(endExclusive(query.getEndDate())));
            }
            if (query.getProductId() != null) {
                result.append(" AND soi.product_id = ?");
                values.add(query.getProductId());
            }
            if (query.getKeyword() != null) {
                result.append(" AND (soi.product_name_snapshot LIKE ? OR p.sku LIKE ?"
                        + " OR p.name LIKE ?)");
                String value = "%" + query.getKeyword() + "%";
                values.add(value); values.add(value); values.add(value);
            }
            return result.toString();
        }

        private static org.threeten.bp.LocalDateTime endExclusive(LocalDate end) {
            return end.plusDays(1L).atStartOfDay();
        }
    }

    private static final class Summary {
        private final long quantity;
        private final BigDecimal amount;

        private Summary(long quantity, BigDecimal amount) {
            this.quantity = quantity;
            this.amount = amount == null ? BigDecimal.ZERO : amount;
        }
    }
}
