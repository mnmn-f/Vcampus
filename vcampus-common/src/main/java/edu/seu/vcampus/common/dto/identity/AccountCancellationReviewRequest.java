package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 管理员审批或驳回账号注销申请；审核人身份由服务端会话提供。 */
public final class AccountCancellationReviewRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long requestId;
    private final String remark;

    public AccountCancellationReviewRequest(long requestId, String remark) {
        this.requestId = requestId;
        this.remark = remark;
    }

    public long getRequestId() { return requestId; }
    public long getId() { return requestId; }
    public String getRemark() { return remark; }
}
