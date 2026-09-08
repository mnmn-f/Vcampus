package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 房间新增或修改请求；occupiedBeds 由服务端从住宿数据计算。 */
public final class DormRoomWriteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long buildingId;
    private final String roomNo;
    private final int floorNo;
    private final int capacity;
    private final String roomType;
    private final String status;
    private final String description;

    public DormRoomWriteRequest(long id, long buildingId, String roomNo, int floorNo,
                                int capacity, String roomType, String status,
                                String description) {
        this.id = id; this.buildingId = buildingId; this.roomNo = roomNo; this.floorNo = floorNo;
        this.capacity = capacity; this.roomType = roomType; this.status = status;
        this.description = description;
    }

    public DormRoomWriteRequest(long buildingId, String roomNo, int floorNo, int capacity,
                                String roomType, String status, String description) {
        this(0L, buildingId, roomNo, floorNo, capacity, roomType, status, description);
    }

    public long getId() { return id; }
    public long getRoomId() { return id; }
    public long getBuildingId() { return buildingId; }
    public String getRoomNo() { return roomNo; }
    public String getRoomNumber() { return roomNo; }
    public int getFloorNo() { return floorNo; }
    public int getCapacity() { return capacity; }
    public String getRoomType() { return roomType; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
}
