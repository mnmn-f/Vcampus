package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 床位新增或修改请求；住宿占用关系不能由此请求伪造。 */
public final class DormBedWriteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long roomId;
    private final String bedNo;
    private final String status;

    public DormBedWriteRequest(long id, long roomId, String bedNo, String status) {
        this.id = id; this.roomId = roomId; this.bedNo = bedNo; this.status = status;
    }

    public DormBedWriteRequest(long roomId, String bedNo, String status) {
        this(0L, roomId, bedNo, status);
    }

    public long getId() { return id; }
    public long getBedId() { return id; }
    public long getRoomId() { return roomId; }
    public String getBedNo() { return bedNo; }
    public String getBedNumber() { return bedNo; }
    public String getStatus() { return status; }
}
