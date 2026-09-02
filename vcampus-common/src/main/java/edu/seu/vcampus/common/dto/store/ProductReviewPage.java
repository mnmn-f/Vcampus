package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 商品评价分页结果。 */
public final class ProductReviewPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<ProductReviewDto> items;
    private final long total;
    public ProductReviewPage(List<ProductReviewDto> items, long total) {
        this.items = Collections.unmodifiableList(new ArrayList<ProductReviewDto>(
                items == null ? Collections.<ProductReviewDto>emptyList() : items));
        this.total = total;
    }
    public List<ProductReviewDto> getItems() { return items; }
    public long getTotal() { return total; }
}
