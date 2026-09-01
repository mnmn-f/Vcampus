package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 销售趋势日期范围，按自然日返回已支付/已完成净统计。 */
public final class StoreSalesTrendQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalDate startDate;
    private final LocalDate endDate;
    public StoreSalesTrendQuery(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("趋势日期范围不正确");
        }
        this.startDate = startDate; this.endDate = endDate;
    }
    public StoreSalesTrendQuery() { this(null, null); }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
