package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 商品评价分页查询。 */
public final class ProductReviewQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final long productId;
    private final int page;
    private final int pageSize;
    public ProductReviewQuery(long productId, int page, int pageSize) {
        if (productId < 0L || page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("评价分页参数不正确");
        }
        this.productId = productId; this.page = page; this.pageSize = pageSize;
    }
    public ProductReviewQuery(long productId) { this(productId, 1, 20); }
    public long getProductId() { return productId; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }
}
