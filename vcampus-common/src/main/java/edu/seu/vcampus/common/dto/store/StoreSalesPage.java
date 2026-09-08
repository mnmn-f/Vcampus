package edu.seu.vcampus.common.dto.store;

import java.math.BigDecimal;
import java.util.List;

/** 销售统计分页结果，页外汇总只包含当前筛选条件的全部结果。 */
public final class StoreSalesPage extends StorePage<StoreSalesDto> {
    private static final long serialVersionUID = 1L;
    private final long totalQuantity;
    private final BigDecimal totalAmount;

    public StoreSalesPage(List<StoreSalesDto> items, int page, int pageSize, long total,
                          long totalQuantity, BigDecimal totalAmount) {
        super(items, page, pageSize, total);
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public StoreSalesPage(int page, int pageSize, long total, List<StoreSalesDto> items,
                          long totalQuantity, BigDecimal totalAmount) {
        this(items, page, pageSize, total, totalQuantity, totalAmount);
    }

    public List<StoreSalesDto> getSales() { return getItems(); }
    public long getTotalQuantity() { return totalQuantity; }
    public long getSoldQuantity() { return totalQuantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getTotalSalesAmount() { return totalAmount; }
}
