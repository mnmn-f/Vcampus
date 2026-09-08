package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.CartItemDto;
import edu.seu.vcampus.common.dto.store.OrderItemDto;

import java.math.BigDecimal;

/** 下单事务中由购物车和商品行共同形成的内部快照。 */
public final class CartLine {
    private final long productId;
    private final String sku;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final int stockQty;
    private final String productStatus;

    public CartLine(long productId, String sku, String productName, BigDecimal unitPrice,
                    int quantity, int stockQty, String productStatus) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.stockQty = stockQty;
        this.productStatus = productStatus;
    }

    public long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public int getStockQty() { return stockQty; }
    public String getProductStatus() { return productStatus; }

    public BigDecimal lineAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** 结算折扣后的内部价格快照；原购物车不被修改。 */
    public CartLine withUnitPrice(BigDecimal price) {
        return new CartLine(productId, sku, productName, price, quantity, stockQty, productStatus);
    }

    public CartItemDto toCartItem() {
        return new CartItemDto(productId, sku, productName, unitPrice, quantity,
                lineAmount(), stockQty, productStatus);
    }

    public OrderItemDto toOrderItem() {
        return new OrderItemDto(productId, productName, unitPrice, quantity, lineAmount());
    }
}
