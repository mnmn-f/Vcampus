package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 学生当前住宿及历史记录视图。 */
public final class AccommodationDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long studentUserId;
    private final long bedId;
    private final long roomId;
    private final long buildingId;
    private final String buildingCode;
    private final String buildingName;
    private final String roomNo;
    private final String bedNo;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String status;

    public AccommodationDto(long id, long studentUserId, long bedId, long roomId,
                            long buildingId, String buildingCode, String buildingName,
                            String roomNo, String bedNo, LocalDate startDate,
                            LocalDate endDate, String status) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.bedId = bedId;
        this.roomId = roomId;
        this.buildingId = buildingId;
        this.buildingCode = buildingCode;
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.bedNo = bedNo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public long getId() { return id; }
    public long getAccommodationId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public long getBedId() { return bedId; }
    public long getRoomId() { return roomId; }
    public long getBuildingId() { return buildingId; }
    public String getBuildingCode() { return buildingCode; }
    public String getBuildingName() { return buildingName; }
    public String getRoomNo() { return roomNo; }
    public String getBedNo() { return bedNo; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
}
