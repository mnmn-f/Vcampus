package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;

/** 可申请教室摘要。 */
public final class CampusClassroomDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String buildingName;
    private final String roomNo;
    private final String classroomType;
    private final int capacity;
    private final String equipmentDescription;
    private final String status;

    public CampusClassroomDto(long id, String buildingName, String roomNo,
                              String classroomType, int capacity,
                              String equipmentDescription, String status) {
        this.id = id;
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.classroomType = classroomType;
        this.capacity = capacity;
        this.equipmentDescription = equipmentDescription;
        this.status = status;
    }

    public CampusClassroomDto(long id, String buildingName, String roomNo,
                              String classroomType, int capacity, String status) {
        this(id, buildingName, roomNo, classroomType, capacity, null, status);
    }

    public long getId() { return id; }
    public long getClassroomId() { return id; }
    public String getBuildingName() { return buildingName; }
    public String getRoomNo() { return roomNo; }
    public String getClassroomType() { return classroomType; }
    public int getCapacity() { return capacity; }
    public String getEquipmentDescription() { return equipmentDescription; }
    public String getStatus() { return status; }
}
