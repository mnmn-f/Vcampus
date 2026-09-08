package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 报修单及其「不在场允许入内」授权。 */
public final class RepairEntryPermitDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long repairOrderId;
    private final long roomId;
    private final String buildingCode;
    private final String roomNo;
    private final String category;
    private final String orderStatus;
    private final LocalDateTime submittedAt;
    private final boolean allowEnter;
    private final String note;
    private final String contactPhone;

    public RepairEntryPermitDto(long repairOrderId, long roomId, String buildingCode,
                                String roomNo, String category, String orderStatus,
                                LocalDateTime submittedAt, boolean allowEnter, String note) {
        this(repairOrderId, roomId, buildingCode, roomNo, category, orderStatus, submittedAt,
                allowEnter, note, null);
    }

    public RepairEntryPermitDto(long repairOrderId, long roomId, String buildingCode,
                                String roomNo, String category, String orderStatus,
                                LocalDateTime submittedAt, boolean allowEnter, String note,
                                String contactPhone) {
        this.repairOrderId = repairOrderId;
        this.roomId = roomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.category = category;
        this.orderStatus = orderStatus;
        this.submittedAt = submittedAt;
        this.allowEnter = allowEnter;
        this.note = note;
        this.contactPhone = contactPhone;
    }

    public long getRepairOrderId() { return repairOrderId; }
    public long getRoomId() { return roomId; }
    public String getBuildingCode() { return buildingCode; }
    public String getRoomNo() { return roomNo; }
    public String getCategory() { return category; }
    public String getOrderStatus() { return orderStatus; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public boolean isAllowEnter() { return allowEnter; }
    public String getNote() { return note; }

    /**
     * 学生在账号里登记的手机号，实时取自 users 表而不落库。
     *
     * <p>维修人员在学生不在场时进门，得有个能当场联系上本人的号码。手机号是
     * 账号资料的一部分，学生改了资料这里就要跟着变，存一份副本只会变成过期数据；
     * 没登记手机号的，授权时强制在备注里留联系方式。</p>
     */
    public String getContactPhone() { return contactPhone; }
}
