package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/** 设置某张报修单的不在场入内授权。 */
public final class RepairEntryPermitRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long repairOrderId;
    private final boolean allowEnter;
    private final String note;

    public RepairEntryPermitRequest(long repairOrderId, boolean allowEnter, String note) {
        this.repairOrderId = repairOrderId;
        this.allowEnter = allowEnter;
        this.note = note;
    }

    public long getRepairOrderId() { return repairOrderId; }
    public boolean isAllowEnter() { return allowEnter; }
    public String getNote() { return note; }
}
