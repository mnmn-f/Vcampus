package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 卫生检查任务：周检查或整改后的复查。 */
public final class HygieneTaskDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_WEEKLY = "WEEKLY";
    public static final String TYPE_RECHECK = "RECHECK";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private final long id;
    private final long roomId;
    private final String buildingCode;
    private final String roomNo;
    private final String taskType;
    private final LocalDate planDate;
    private final String status;
    private final Long inspectionId;
    private final Long sourceInspectionId;

    public HygieneTaskDto(long id, long roomId, String buildingCode, String roomNo,
                          String taskType, LocalDate planDate, String status,
                          Long inspectionId, Long sourceInspectionId) {
        this.id = id;
        this.roomId = roomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.taskType = taskType;
        this.planDate = planDate;
        this.status = status;
        this.inspectionId = inspectionId;
        this.sourceInspectionId = sourceInspectionId;
    }

    public long getId() { return id; }
    public long getRoomId() { return roomId; }
    public String getBuildingCode() { return buildingCode; }
    public String getRoomNo() { return roomNo; }
    public String getTaskType() { return taskType; }
    public LocalDate getPlanDate() { return planDate; }
    public String getStatus() { return status; }
    public Long getInspectionId() { return inspectionId; }
    public Long getSourceInspectionId() { return sourceInspectionId; }

    public boolean isRecheck() { return TYPE_RECHECK.equals(taskType); }
}
