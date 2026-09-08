package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** 房间水电抄表读数视图；账单生成的输入。 */
public final class MeterReadingDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long roomId;
    private final String buildingCode;
    private final String roomNo;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final BigDecimal electricityUnits;
    private final BigDecimal waterUnits;
    private final BigDecimal electricityPrice;
    private final BigDecimal waterPrice;
    private final BigDecimal totalAmount;
    private final long recordedBy;
    private final LocalDateTime recordedAt;
    private final Long billId;

    public MeterReadingDto(long id, long roomId, String buildingCode, String roomNo,
                           LocalDate periodStart, LocalDate periodEnd,
                           BigDecimal electricityUnits, BigDecimal waterUnits,
                           BigDecimal electricityPrice, BigDecimal waterPrice,
                           long recordedBy, LocalDateTime recordedAt, Long billId) {
        this.id = id;
        this.roomId = roomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.electricityUnits = electricityUnits;
        this.waterUnits = waterUnits;
        this.electricityPrice = electricityPrice;
        this.waterPrice = waterPrice;
        this.totalAmount = amount(electricityUnits, electricityPrice)
                .add(amount(waterUnits, waterPrice))
                .setScale(2, RoundingMode.HALF_UP);
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
        this.billId = billId;
    }

    /** 读数乘单价；任一侧为空按零计，避免视图层再做空值判断。 */
    private static BigDecimal amount(BigDecimal units, BigDecimal price) {
        if (units == null || price == null) return BigDecimal.ZERO;
        return units.multiply(price);
    }

    public long getId() { return id; }
    public long getRoomId() { return roomId; }
    public String getBuildingCode() { return buildingCode; }
    public String getRoomNo() { return roomNo; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getElectricityUnits() { return electricityUnits; }
    public BigDecimal getWaterUnits() { return waterUnits; }
    public BigDecimal getElectricityPrice() { return electricityPrice; }
    public BigDecimal getWaterPrice() { return waterPrice; }
    /** 按读数与单价折算的房间应缴总额。 */
    public BigDecimal getTotalAmount() { return totalAmount; }
    public long getRecordedBy() { return recordedBy; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    /** 已生成账单时为账单编号，未生成为 null。 */
    public Long getBillId() { return billId; }
    /** 已生成账单的读数不允许再改。 */
    public boolean isLocked() { return billId != null; }
}
