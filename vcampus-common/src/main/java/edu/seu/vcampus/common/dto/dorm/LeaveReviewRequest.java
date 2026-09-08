package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 宿管审核请假申请；approved=false 表示拒绝。 */
public final class LeaveReviewRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long leaveId;
    private final boolean approved;
    private final String remark;

    public LeaveReviewRequest(long leaveId, boolean approved, String remark) {
        this.leaveId = leaveId; this.approved = approved; this.remark = remark;
    }

    public long getLeaveId() { return leaveId; }
    public long getRequestId() { return leaveId; }
    public long getId() { return leaveId; }
    public boolean isApproved() { return approved; }
    public String getRemark() { return remark; }
}
