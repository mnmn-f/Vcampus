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

    protected ProductData(String sku, String name, String category, String description,
                          BigDecimal price, int stockQty, String status) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.stockQty = stockQty;
        this.status = status;
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
}
