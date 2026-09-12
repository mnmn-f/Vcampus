package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** 水电分摊视图，包含账单、学生和本人分摊状态。 */
public final class UtilityBillDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long allocationId;
    private final long billId;
    private final long roomId;
    private final Long studentUserId;
    private final String roomNo;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final BigDecimal electricityUnits;
    private final BigDecimal waterUnits;
    private final BigDecimal totalAmount;
    private final BigDecimal allocatedAmount;
    private final String billStatus;
    private final String allocationStatus;
    private final LocalDateTime dueAt;
    private final Long paidTransactionId;
    private final LocalDateTime paidAt;
    private final String studentLabel;

    public UtilityBillDto(long allocationId, long billId, long roomId, String roomNo,
                          LocalDate periodStart, LocalDate periodEnd,
                          BigDecimal electricityUnits, BigDecimal waterUnits,
                          BigDecimal totalAmount, BigDecimal allocatedAmount,
                          String billStatus, String allocationStatus,
                          LocalDateTime dueAt, Long paidTransactionId,
                          LocalDateTime paidAt) {
        this(allocationId, billId, roomId, null, roomNo, periodStart, periodEnd,
                electricityUnits, waterUnits, totalAmount, allocatedAmount, billStatus,
                allocationStatus, dueAt, paidTransactionId, paidAt);
    }

    public UtilityBillDto(long allocationId, long billId, long roomId, Long studentUserId,
                          String roomNo, LocalDate periodStart, LocalDate periodEnd,
                          BigDecimal electricityUnits, BigDecimal waterUnits,
                          BigDecimal totalAmount, BigDecimal allocatedAmount,
                          String billStatus, String allocationStatus,
                          LocalDateTime dueAt, Long paidTransactionId,
                          LocalDateTime paidAt) {
        this(allocationId, billId, roomId, studentUserId, roomNo, periodStart, periodEnd, electricityUnits, waterUnits,
                totalAmount, allocatedAmount, billStatus, allocationStatus, dueAt, paidTransactionId, paidAt, null);
    }
    public UtilityBillDto(long allocationId, long billId, long roomId, Long studentUserId, String roomNo,
            LocalDate periodStart, LocalDate periodEnd, BigDecimal electricityUnits, BigDecimal waterUnits,
            BigDecimal totalAmount, BigDecimal allocatedAmount, String billStatus, String allocationStatus,
            LocalDateTime dueAt, Long paidTransactionId, LocalDateTime paidAt, String studentLabel) {
        this.studentLabel = studentLabel;
        this.allocationId = allocationId;
        this.billId = billId;
        this.roomId = roomId;
        this.studentUserId = studentUserId;
        this.roomNo = roomNo;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.electricityUnits = electricityUnits;
        this.waterUnits = waterUnits;
        this.totalAmount = totalAmount;
        this.allocatedAmount = allocatedAmount;
        this.billStatus = billStatus;
        this.allocationStatus = allocationStatus;
        this.dueAt = dueAt;
        this.paidTransactionId = paidTransactionId;
        this.paidAt = paidAt;
    }

    public long getAllocationId() { return allocationId; }
    public long getId() { return allocationId; }
    public long getBillId() { return billId; }
    public long getRoomId() { return roomId; }
    public Long getStudentUserId() { return studentUserId; }
    public String getRoomNo() { return roomNo; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getElectricityUnits() { return electricityUnits; }
    public BigDecimal getWaterUnits() { return waterUnits; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public String getBillStatus() { return billStatus; }
    public String getAllocationStatus() { return allocationStatus; }
    public LocalDateTime getDueAt() { return dueAt; }
    public Long getPaidTransactionId() { return paidTransactionId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getStudentLabel() { return studentLabel; }
}
