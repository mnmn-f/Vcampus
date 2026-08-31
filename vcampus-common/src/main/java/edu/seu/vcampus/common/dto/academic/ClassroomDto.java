package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 排课使用的教室摘要。 */
public final class ClassroomDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String buildingName;
    private final String roomNo;
    private final String classroomType;
    private final int capacity;
    private final String status;

    public ClassroomDto(long id, String buildingName, String roomNo,
                        String classroomType, int capacity, String status) {
        this.id = id;
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.classroomType = classroomType;
        this.capacity = capacity;
        this.status = status;
    }

    public long getId() { return id; }
    public String getBuildingName() { return buildingName; }
    public String getRoomNo() { return roomNo; }
    public String getClassroomType() { return classroomType; }
    public int getCapacity() { return capacity; }
    public String getStatus() { return status; }
}
