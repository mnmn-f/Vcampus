package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/**
 * 宿管审批动作；不得携带可替换的申请人身份。
 *
 * <p>{@code bedId} 是宿管在审批时点的那张床。学生提交入住/调宿申请时不再自己填床位
 * ——他既不知道哪张床空着，也不该替宿管决定住哪儿——所以「批准」和「分配到哪张床」
 * 是同一次操作的两半，必须一起传上来，由服务端在一个事务里落实。</p>
 */
public final class DormApprovalRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long requestId;
    private final boolean approved;
    private final String remark;
    private final Long bedId;

    public DormApprovalRequest(long requestId, boolean approved, String remark) {
        this(requestId, approved, remark, null);
    }

    public DormApprovalRequest(long requestId, boolean approved, String remark, Long bedId) {
        this.requestId = requestId;
        this.approved = approved;
        this.remark = remark;
        this.bedId = bedId;
    }

    public long getRequestId() { return requestId; }
    public boolean isApproved() { return approved; }
    public String getRemark() { return remark; }

    /** 宿管指定的床位；退宿申请为 {@code null}。 */
    public Long getBedId() { return bedId; }
}
