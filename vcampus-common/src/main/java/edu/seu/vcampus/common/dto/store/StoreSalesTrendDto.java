package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDate;

/** 单个自然日销售趋势点。 */
public final class StoreSalesTrendDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalDate date;
    private final long quantity;
    private final BigDecimal amount;
    public StoreSalesTrendDto(LocalDate date, long quantity, BigDecimal amount) {
        this.date = date; this.quantity = quantity;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }
    public LocalDate getDate() { return date; }
    public long getQuantity() { return quantity; }
    public BigDecimal getAmount() { return amount; }
}
