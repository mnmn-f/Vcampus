package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class MemoryReviewCandidates {
    private MemoryReviewCandidates() { }
    static ReviewCandidatePage load(StoreRecordRepository core, Collection<ProductReviewDto> reviews,
            Connection c, long user, ProductReviewQuery query) {
        ProductReviewQuery q = query == null ? new ProductReviewQuery(0) : query;
        List<ReviewCandidateDto> rows = new ArrayList<>();
        for (int page = 1; ; page++) {
            OrderPage orders = core.findOrders(c, user, new OrderQuery(null, user, "COMPLETED", page, 100));
            for (OrderDto order : orders.getItems()) for (OrderItemDto item : order.getItems()) {
                if (q.getProductId() > 0 && q.getProductId() != item.getProductId()) continue;
                String text = order.getOrderNo() + " " + item.getProductName();
                if (q.getKeyword() != null && !text.toLowerCase(Locale.ROOT).contains(q.getKeyword().toLowerCase(Locale.ROOT))) continue;
                boolean reviewed = false;
                for (ProductReviewDto review : reviews) if (review.getOrderId() == order.getId() && review.getProductId() == item.getProductId()) { reviewed = true; break; }
                if (!reviewed) rows.add(new ReviewCandidateDto(order.getId(), item.getProductId(), order.getOrderNo(), item.getProductName(), item.getQuantity()));
            }
            if (orders.getItems().isEmpty() || (long) page * 100 >= orders.getTotal()) break;
        }
        rows.sort(Comparator.comparingLong(ReviewCandidateDto::getOrderId).reversed());
        int start = Math.min(rows.size(), q.getOffset());
        return new ReviewCandidatePage(rows.subList(start, Math.min(rows.size(), start + q.getPageSize())), rows.size());
    }
    static ProductReviewPage page(Collection<ProductReviewDto> source, ProductReviewQuery query) {
        ProductReviewQuery q = query == null ? new ProductReviewQuery(0) : query; List<ProductReviewDto> rows = new ArrayList<>();
        for (ProductReviewDto row : source) if ((q.getProductId() <= 0 || q.getProductId() == row.getProductId())
                && (q.getKeyword() == null || String.valueOf(row.getContent()).toLowerCase(Locale.ROOT).contains(q.getKeyword().toLowerCase(Locale.ROOT)))) rows.add(row);
        rows.sort(Comparator.comparingLong(ProductReviewDto::getId).reversed());
        int start = Math.min(rows.size(), q.getOffset());
        return new ProductReviewPage(rows.subList(start, Math.min(rows.size(), start + q.getPageSize())), rows.size());
    }
}
