package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;

/** 商品读写对象共享的业务字段。 */
public abstract class ProductData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sku;
    private final String name;
    private final String category;
    private final String description;
    private final BigDecimal price;
    private final int stockQty;
    private final String status;
    private final String imageUrl;
    private final BigDecimal ratingAverage;
    private final long ratingCount;

    protected ProductData(String sku, String name, String category, String description,
                          BigDecimal price, int stockQty, String status) {
        this(sku, name, category, description, price, stockQty, status,
                null, BigDecimal.ZERO, 0L);
    }

    protected ProductData(String sku, String name, String category, String description,
                          BigDecimal price, int stockQty, String status, String imageUrl,
                          BigDecimal ratingAverage, long ratingCount) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.stockQty = stockQty;
        this.status = status;
        this.imageUrl = imageUrl;
        this.ratingAverage = ratingAverage == null ? BigDecimal.ZERO : ratingAverage;
        this.ratingCount = ratingCount;
    }

    public final String getSku() { return sku; }
    public final String getCode() { return sku; }
    public final String getProductCode() { return sku; }
    public final String getName() { return name; }
    public final String getProductName() { return name; }
    public final String getCategory() { return category; }
    public final String getDescription() { return description; }
    public final BigDecimal getPrice() { return price; }
    public final int getStockQty() { return stockQty; }
    public final int getStockQuantity() { return stockQty; }
    public final int getStock() { return stockQty; }
    public final String getStatus() { return status; }
    public final String getImageUrl() { return imageUrl; }
    public final BigDecimal getRatingAverage() { return ratingAverage; }
    public final long getRatingCount() { return ratingCount; }
}
