package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 学生提交请假申请；studentUserId 不在请求中出现。 */
public final class LeaveSubmitRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String leaveType;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String reason;

    public LeaveSubmitRequest(String leaveType, LocalDateTime startAt,
                              LocalDateTime endAt, String reason) {
        this.leaveType = leaveType; this.startAt = startAt; this.endAt = endAt; this.reason = reason;
    }

    public LeaveSubmitRequest(DormLeaveType leaveType, LocalDateTime startAt,
                              LocalDateTime endAt, String reason) {
        this(leaveType == null ? null : leaveType.name(), startAt, endAt, reason);
    }

    public String getLeaveType() { return leaveType; }
    public String getType() { return leaveType; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getStartTime() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public LocalDateTime getEndTime() { return endAt; }
    public String getReason() { return reason; }
}
