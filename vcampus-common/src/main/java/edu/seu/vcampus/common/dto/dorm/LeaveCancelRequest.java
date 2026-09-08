package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 学生取消本人待审核申请。 */
public final class LeaveCancelRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long leaveId;

    public LeaveCancelRequest(long leaveId) { this.leaveId = leaveId; }
    public long getLeaveId() { return leaveId; }
    public long getId() { return leaveId; }
    public long getRequestId() { return leaveId; }
}
