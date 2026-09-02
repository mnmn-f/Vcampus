package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/** 按检查编号查询分项明细。 */
public final class HygieneDetailRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long inspectionId;

    public HygieneDetailRequest(long inspectionId) { this.inspectionId = inspectionId; }

    public long getInspectionId() { return inspectionId; }
}
