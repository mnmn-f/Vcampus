package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;

/** 按商品汇总的销售统计行。 */
public final class StoreSalesDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long productId;
    private final String sku;
    private final String productName;
    private final long quantitySold;
    private final BigDecimal salesAmount;

    public StoreSalesDto(long productId, String sku, String productName,
                         long quantitySold, BigDecimal salesAmount) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.salesAmount = salesAmount == null ? BigDecimal.ZERO : salesAmount;
    }

    public long getProductId() { return productId; }
    public long getId() { return productId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public String getName() { return productName; }
    public long getQuantitySold() { return quantitySold; }
    public long getQuantity() { return quantitySold; }
    public long getSoldQuantity() { return quantitySold; }
    public BigDecimal getSalesAmount() { return salesAmount; }
    public BigDecimal getAmount() { return salesAmount; }
    public BigDecimal getTotalAmount() { return salesAmount; }
}
