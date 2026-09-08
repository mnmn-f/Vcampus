package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/**
 * 学生当前在宿状态。
 *
 * <p>对应设计文档 DormAssignment 上的 stayStatus。这里没有把它落成住宿记录上的
 * 一列：状态完全由门禁流水和已批准的请假推导，冗余存一份就必然出现「流水显示
 * 人在外面、状态字段却写着在宿」的不一致，而修这种不一致比重新算一次贵得多。</p>
 */
public final class StayStatusDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String IN_DORM = "IN_DORM";
    public static final String OUT = "OUT";
    public static final String LEAVE_REGISTERED = "LEAVE_REGISTERED";

    private final long studentUserId;
    private final long roomId;
    private final String buildingCode;
    private final String roomNo;
    private final String status;
    private final LocalDateTime lastExitAt;
    private final LocalDateTime lastEntryAt;

    public StayStatusDto(long studentUserId, long roomId, String buildingCode, String roomNo,
                         String status, LocalDateTime lastExitAt, LocalDateTime lastEntryAt) {
        this.studentUserId = studentUserId;
        this.roomId = roomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.status = status;
        this.lastExitAt = lastExitAt;
        this.lastEntryAt = lastEntryAt;
    }

    public long getStudentUserId() { return studentUserId; }
    public long getRoomId() { return roomId; }
    public String getBuildingCode() { return buildingCode; }
    public String getRoomNo() { return roomNo; }
    public String getStatus() { return status; }
    public LocalDateTime getLastExitAt() { return lastExitAt; }
    public LocalDateTime getLastEntryAt() { return lastEntryAt; }

    public static String statusName(String status) {
        if (OUT.equals(status)) return "离宿";
        if (LEAVE_REGISTERED.equals(status)) return "离校登记中";
        return "在宿";
    }
}
