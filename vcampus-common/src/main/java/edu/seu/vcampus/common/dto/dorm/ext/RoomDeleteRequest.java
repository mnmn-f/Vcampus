package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/** 删除空置房间。 */
public final class RoomDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long roomId;

    public RoomDeleteRequest(long roomId) { this.roomId = roomId; }

    public long getRoomId() { return roomId; }
}
