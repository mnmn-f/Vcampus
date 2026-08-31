package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;

/** 订单商品快照；价格来自下单时的数据库商品价格。 */
public final class OrderItemDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal lineAmount;

    public OrderItemDto(long productId, String productName, BigDecimal unitPrice,
                        int quantity, BigDecimal lineAmount) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineAmount = lineAmount;
    }

    public OrderItemDto(long productId, String productName, BigDecimal unitPrice,
                        int quantity) {
        this(productId, productName, unitPrice, quantity,
                unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineAmount() { return lineAmount; }
}
