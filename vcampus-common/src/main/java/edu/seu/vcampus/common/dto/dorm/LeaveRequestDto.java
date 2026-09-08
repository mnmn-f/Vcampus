package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 宿舍请假申请视图；申请人和审核人仅由服务端填充。 */
public final class LeaveRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long studentUserId;
    private final String leaveType;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String reason;
    private final String status;
    private final Long reviewedBy;
    private final LocalDateTime reviewedAt;
    private final String reviewRemark;
    private final LocalDateTime createdAt;

    public LeaveRequestDto(long id, long studentUserId, String leaveType,
                           LocalDateTime startAt, LocalDateTime endAt, String reason,
                           String status, Long reviewedBy, LocalDateTime reviewedAt,
                           String reviewRemark, LocalDateTime createdAt) {
        this.id = id; this.studentUserId = studentUserId; this.leaveType = leaveType;
        this.startAt = startAt; this.endAt = endAt; this.reason = reason; this.status = status;
        this.reviewedBy = reviewedBy; this.reviewedAt = reviewedAt;
        this.reviewRemark = reviewRemark; this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getLeaveId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public String getLeaveType() { return leaveType; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public Long getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewRemark() { return reviewRemark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
