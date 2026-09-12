package edu.seu.vcampus.common.dto.store;

import java.math.BigDecimal;

/** 商品新增、编辑和库存维护请求；id 为 0 表示新增。 */
public final class ProductWriteRequest extends ProductData {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final byte[] imageData;

    public ProductWriteRequest(long id, String sku, String name, String category,
                               String description, BigDecimal price, int stockQty,
                               String status) {
        this(id, sku, name, category, description, price, stockQty, status, null);
    }

    public ProductWriteRequest(long id, String sku, String name, String category,
                               String description, BigDecimal price, int stockQty,
                               String status, String imageUrl) {
        this(id, sku, name, category, description, price, stockQty, status, imageUrl, null);
    }

    public ProductWriteRequest(long id, String sku, String name, String category,
            String description, BigDecimal price, int stockQty, String status, String imageUrl, byte[] imageData) {
        super(sku, name, category, description, price, stockQty, status,
                imageData == null ? imageUrl : "store-image:" + java.util.UUID.randomUUID(),
                BigDecimal.ZERO, 0L);
        this.id = id;
        this.imageData = imageData == null ? null : imageData.clone();
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
    public byte[] getImageData() { return imageData == null ? null : imageData.clone(); }
}
