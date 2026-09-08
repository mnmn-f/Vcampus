package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 生成周检查任务；楼栋留空表示全部楼栋。 */
public final class HygieneTaskGenerateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long buildingId;
    private final LocalDate planDate;

    public HygieneTaskGenerateRequest(Long buildingId, LocalDate planDate) {
        this.buildingId = buildingId;
        this.planDate = planDate;
    }

    public Long getBuildingId() { return buildingId; }
    /** 计划检查日期；留空由服务端取当天。 */
    public LocalDate getPlanDate() { return planDate; }
}
