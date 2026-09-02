package edu.seu.vcampus.common.dto.store;

import java.math.BigDecimal;

/** 商品新增、编辑和库存维护请求；id 为 0 表示新增。 */
public final class ProductWriteRequest extends ProductData {
    private static final long serialVersionUID = 1L;
    private final long id;

    public ProductWriteRequest(long id, String sku, String name, String category,
                               String description, BigDecimal price, int stockQty,
                               String status) {
        this(id, sku, name, category, description, price, stockQty, status, null);
    }

    public ProductWriteRequest(long id, String sku, String name, String category,
                               String description, BigDecimal price, int stockQty,
                               String status, String imageUrl) {
        super(sku, name, category, description, price, stockQty, status, imageUrl,
                BigDecimal.ZERO, 0L);
        this.id = id;
    }

    public ProductWriteRequest(long id, String sku, String name, String category,
                               BigDecimal price, int stockQty, String status) {
        this(id, sku, name, category, null, price, stockQty, status);
    }

    public ProductWriteRequest(String sku, String name, String category,
                               String description, BigDecimal price, int stockQty,
                               String status) {
        this(0L, sku, name, category, description, price, stockQty, status);
    }

    public ProductWriteRequest(String sku, String name, String category,
                               String description, BigDecimal price, int stockQty,
                               String status, String imageUrl) {
        this(0L, sku, name, category, description, price, stockQty, status, imageUrl);
    }

    public ProductWriteRequest(String sku, String name, String category,
                               BigDecimal price, int stockQty, String status) {
        this(0L, sku, name, category, null, price, stockQty, status);
    }

    public long getId() { return id; }
    public long getProductId() { return id; }
}
