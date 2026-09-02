package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;

/** 结算预览中的商品行；价格由服务端读取。 */
public final class CheckoutLineDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineAmount;
    private final int stockQty;

    public CheckoutLineDto(long productId, String productName, int quantity,
                           BigDecimal unitPrice, BigDecimal lineAmount, int stockQty) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineAmount = lineAmount;
        this.stockQty = stockQty;
    }
    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineAmount() { return lineAmount; }
    public int getStockQty() { return stockQty; }
}
