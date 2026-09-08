package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalTime;

/** 自习室公开信息。 */
public final class StudyRoomView implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String buildingName;
    private final String roomNo;
    private final int capacity;
    private final LocalTime openTime;
    private final LocalTime closeTime;
    private final String status;
    private final String description;

    public StudyRoomView(long id, String buildingName, String roomNo, int capacity,
                         LocalTime openTime, LocalTime closeTime, String status,
                         String description) {
        this.id = id;
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.capacity = capacity;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.status = status;
        this.description = description;
    }

    public long getId() { return id; }
    public String getBuildingName() { return buildingName; }
    public String getRoomNo() { return roomNo; }
    public int getCapacity() { return capacity; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
}
