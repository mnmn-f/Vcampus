package edu.seu.vcampus.common.dto.store;

import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 商品详情和检索列表的跨端对象。 */
public final class ProductDto extends ProductData {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ProductDto(long id, String sku, String name, String category, String description,
                      BigDecimal price, int stockQty, String status) {
        this(id, sku, name, category, description, price, stockQty, status,
                null, null, null);
    }

    public ProductDto(long id, String sku, String name, String category,
                      BigDecimal price, int stockQty, String status) {
        this(id, sku, name, category, null, price, stockQty, status);
    }

    public ProductDto(long id, String sku, String name, String category, String description,
                      BigDecimal price, int stockQty, String status, Long createdBy,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, sku, name, category, description, price, stockQty, status, null,
                BigDecimal.ZERO, 0L, createdBy, createdAt, updatedAt);
    }

    public ProductDto(long id, String sku, String name, String category, String description,
                      BigDecimal price, int stockQty, String status, String imageUrl,
                      BigDecimal ratingAverage, long ratingCount, Long createdBy,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(sku, name, category, description, price, stockQty, status, imageUrl,
                ratingAverage, ratingCount);
        this.id = id;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public long getProductId() { return id; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
