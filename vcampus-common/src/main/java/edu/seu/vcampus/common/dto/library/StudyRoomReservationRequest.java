package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 自习室预约时段；预约人由服务端会话确定。 */
public final class StudyRoomReservationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long roomId;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;

    public StudyRoomReservationRequest(long roomId, LocalDateTime startAt,
                                       LocalDateTime endAt) {
        this.roomId = roomId;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public long getRoomId() { return roomId; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
}
