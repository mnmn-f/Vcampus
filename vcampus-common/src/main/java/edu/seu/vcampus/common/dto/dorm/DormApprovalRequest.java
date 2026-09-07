package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 宿管审批动作；不得携带可替换的申请人身份。 */
public final class DormApprovalRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long requestId;
    private final boolean approved;
    private final String remark;

    public DormApprovalRequest(long requestId, boolean approved, String remark) {
        this.requestId = requestId;
        this.approved = approved;
        this.remark = remark;
    }

    public long getRequestId() { return requestId; }
    public boolean isApproved() { return approved; }
    public String getRemark() { return remark; }
}
