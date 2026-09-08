package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 周检查任务生成结果。 */
public final class HygieneTaskGenerateResultDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalDate planDate;
    private final int roomsScanned;
    private final int created;
    private final int existing;

    public HygieneTaskGenerateResultDto(LocalDate planDate, int roomsScanned, int created,
                                        int existing) {
        this.planDate = planDate;
        this.roomsScanned = roomsScanned;
        this.created = created;
        this.existing = existing;
    }

    public LocalDate getPlanDate() { return planDate; }
    public int getRoomsScanned() { return roomsScanned; }
    public int getCreated() { return created; }
    /** 已存在因而跳过的任务数；重复生成时全部落在这里。 */
    public int getExisting() { return existing; }
}
