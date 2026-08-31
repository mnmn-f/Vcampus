package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;

/** 购物车中商品及其当前价格、库存快照。 */
public final class CartItemDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long productId;
    private final String sku;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal lineAmount;
    private final int stockQty;
    private final String productStatus;

    public CartItemDto(long productId, String sku, String productName,
                       BigDecimal unitPrice, int quantity, BigDecimal lineAmount,
                       int stockQty, String productStatus) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineAmount = lineAmount;
        this.stockQty = stockQty;
        this.productStatus = productStatus;
    }

    public CartItemDto(long productId, String sku, String productName,
                       BigDecimal unitPrice, int quantity) {
        this(productId, sku, productName, unitPrice, quantity,
                unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(quantity)),
                0, null);
    }

    public long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getProductCode() { return sku; }
    public String getProductName() { return productName; }
    public String getName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineAmount() { return lineAmount; }
    public int getStockQty() { return stockQty; }
    public int getStock() { return stockQty; }
    public String getProductStatus() { return productStatus; }
}
