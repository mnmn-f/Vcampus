package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 房间查询视图。 */
public final class DormRoomDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long buildingId;
    private final String buildingCode;
    private final String buildingName;
    private final String roomNo;
    private final int floorNo;
    private final int capacity;
    private final String roomType;
    private final String status;
    private final String description;
    private final int occupiedBeds;

    public DormRoomDto(long id, long buildingId, String buildingCode, String buildingName,
                       String roomNo, int floorNo, int capacity, String roomType,
                       String status, String description, int occupiedBeds) {
        this.id = id;
        this.buildingId = buildingId;
        this.buildingCode = buildingCode;
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.floorNo = floorNo;
        this.capacity = capacity;
        this.roomType = roomType;
        this.status = status;
        this.description = description;
        this.occupiedBeds = occupiedBeds;
    }

    public long getId() { return id; }
    public long getRoomId() { return id; }
    public long getBuildingId() { return buildingId; }
    public String getBuildingCode() { return buildingCode; }
    public String getBuildingName() { return buildingName; }
    public String getRoomNo() { return roomNo; }
    public int getFloorNo() { return floorNo; }
    public int getCapacity() { return capacity; }
    public String getRoomType() { return roomType; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public int getOccupiedBeds() { return occupiedBeds; }
}
