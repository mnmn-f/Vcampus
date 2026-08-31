package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.OrderItemDto;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.dto.store.StoreSalesDto;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;

import java.sql.Connection;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 基于订单快照的内存销售统计，和 MySQL 统计使用相同状态及日期语义。 */
final class InMemoryStoreSalesRepository implements StoreSalesRepository {
    private final InMemoryStoreState state;

    InMemoryStoreSalesRepository(InMemoryStoreState state) { this.state = state; }

    @Override
    public StoreSalesPage findSales(Connection connection, StoreSalesQuery query) {
        StoreSalesQuery q = query == null ? new StoreSalesQuery() : query;
        synchronized (state) {
            Map<Long, Aggregate> aggregates = new LinkedHashMap<Long, Aggregate>();
            for (InMemoryStoreState.MemoryOrder order : state.orders.values()) {
                if (!included(order.status) || !inRange(order.paidAt, q)) continue;
                for (OrderItemDto item : order.items) {
                    ProductDto product = state.products.get(item.getProductId());
                    if (!matches(product, item, q)) continue;
                    Aggregate aggregate = aggregates.get(item.getProductId());
                    if (aggregate == null) {
                        aggregate = new Aggregate(item.getProductId(), product, item.getProductName());
                        aggregates.put(item.getProductId(), aggregate);
                    }
                    aggregate.add(item);
                }
            }
            List<StoreSalesDto> all = new ArrayList<StoreSalesDto>();
            long quantity = 0L;
            java.math.BigDecimal amount = java.math.BigDecimal.ZERO;
            for (Aggregate aggregate : aggregates.values()) {
                StoreSalesDto row = aggregate.toDto();
                all.add(row);
                quantity += row.getQuantitySold();
                amount = amount.add(row.getSalesAmount());
            }
            Collections.sort(all, new Comparator<StoreSalesDto>() {
                @Override public int compare(StoreSalesDto left, StoreSalesDto right) {
                    int amount = right.getSalesAmount().compareTo(left.getSalesAmount());
                    return amount == 0 ? Long.compare(left.getProductId(), right.getProductId()) : amount;
                }
            });
            int from = Math.min(q.getOffset(), all.size());
            int to = Math.min(from + q.getPageSize(), all.size());
            return new StoreSalesPage(new ArrayList<StoreSalesDto>(all.subList(from, to)),
                    q.getPage(), q.getPageSize(), all.size(), quantity, amount);
        }
    }

    private static boolean included(String status) {
        return "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
    }

    private static boolean inRange(LocalDateTime paidAt, StoreSalesQuery q) {
        if (paidAt == null) return false;
        LocalDate start = q.getStartDate();
        LocalDate end = q.getEndDate();
        return (start == null || !paidAt.isBefore(start.atStartOfDay()))
                && (end == null || paidAt.isBefore(end.plusDays(1L).atStartOfDay()));
    }

    private static boolean matches(ProductDto product, OrderItemDto item, StoreSalesQuery q) {
        if (q.getProductId() != null && q.getProductId().longValue() != item.getProductId()) return false;
        if (q.getKeyword() == null) return true;
        String keyword = q.getKeyword().toLowerCase(Locale.ROOT);
        return contains(product == null ? null : product.getSku(), keyword)
                || contains(product == null ? null : product.getName(), keyword)
                || contains(item.getProductName(), keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static final class Aggregate {
        private final long productId;
        private final String sku;
        private final String productName;
        private long quantity;
        private java.math.BigDecimal amount = java.math.BigDecimal.ZERO;

        private Aggregate(long productId, ProductDto product, String snapshotName) {
            this.productId = productId;
            this.sku = product == null ? null : product.getSku();
            this.productName = product == null ? snapshotName : product.getName();
        }

        private void add(OrderItemDto item) {
            quantity += item.getQuantity();
            java.math.BigDecimal line = item.getLineAmount();
            if (line == null && item.getUnitPrice() != null) {
                line = item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()));
            }
            if (line != null) amount = amount.add(line);
        }

        private StoreSalesDto toDto() {
            return new StoreSalesDto(productId, sku, productName, quantity, amount);
        }
    }
}
