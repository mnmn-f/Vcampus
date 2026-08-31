package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 床位查询视图；occupantUserId 仅对有管理权限的响应填充。 */
public final class DormBedDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long roomId;
    private final String buildingCode;
    private final String roomNo;
    private final String bedNo;
    private final String status;
    private final Long occupantUserId;

    public DormBedDto(long id, long roomId, String buildingCode, String roomNo,
                      String bedNo, String status, Long occupantUserId) {
        this.id = id;
        this.roomId = roomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.bedNo = bedNo;
        this.status = status;
        this.occupantUserId = occupantUserId;
    }

    public long getId() { return id; }
    public long getBedId() { return id; }
    public long getRoomId() { return roomId; }
    public String getBuildingCode() { return buildingCode; }
    public String getRoomNo() { return roomNo; }
    public String getBedNo() { return bedNo; }
    public String getStatus() { return status; }
    public Long getOccupantUserId() { return occupantUserId; }
}
