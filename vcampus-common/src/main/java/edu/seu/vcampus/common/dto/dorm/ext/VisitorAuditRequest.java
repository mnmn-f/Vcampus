package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/** 宿管对来访登记的审核结论，也用于学生撤销（此时只用 registrationId）。 */
public final class VisitorAuditRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long registrationId;
    private final boolean approved;
    private final String remark;

    public VisitorAuditRequest(long registrationId, boolean approved, String remark) {
        this.registrationId = registrationId;
        this.approved = approved;
        this.remark = remark;
    }

    public long getRegistrationId() { return registrationId; }
    public boolean isApproved() { return approved; }
    public String getRemark() { return remark; }
}
