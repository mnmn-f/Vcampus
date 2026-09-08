package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDate;

/** 抄表读数录入请求；同一房间同一账期重复提交按更新处理。 */
public final class MeterReadingRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long roomId;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final BigDecimal electricityUnits;
    private final BigDecimal waterUnits;
    private final BigDecimal electricityPrice;
    private final BigDecimal waterPrice;

    public MeterReadingRequest(long roomId, LocalDate periodStart, LocalDate periodEnd,
                               BigDecimal electricityUnits, BigDecimal waterUnits,
                               BigDecimal electricityPrice, BigDecimal waterPrice) {
        this.roomId = roomId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.electricityUnits = electricityUnits;
        this.waterUnits = waterUnits;
        this.electricityPrice = electricityPrice;
        this.waterPrice = waterPrice;
    }

    public long getRoomId() { return roomId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getElectricityUnits() { return electricityUnits; }
    public BigDecimal getWaterUnits() { return waterUnits; }
    public BigDecimal getElectricityPrice() { return electricityPrice; }
    public BigDecimal getWaterPrice() { return waterPrice; }
}
