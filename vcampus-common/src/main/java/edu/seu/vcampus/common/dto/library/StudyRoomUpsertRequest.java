package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalTime;

/** 图书管理员新增或维护自习室；id 为 0 表示新增。 */
public final class StudyRoomUpsertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String buildingName;
    private final String roomNo;
    private final int capacity;
    private final LocalTime openTime;
    private final LocalTime closeTime;
    private final String status;
    private final String description;

    public StudyRoomUpsertRequest(long id, String buildingName, String roomNo,
                                  int capacity, LocalTime openTime,
                                  LocalTime closeTime, String status,
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

    public StudyRoomUpsertRequest(String buildingName, String roomNo, int capacity,
                                  LocalTime openTime, LocalTime closeTime,
                                  String description) {
        this(0L, buildingName, roomNo, capacity, openTime, closeTime,
                "OPEN", description);
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
