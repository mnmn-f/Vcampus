package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 自习室预约记录。 */
public final class StudyRoomReservationView implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long roomId;
    private final String roomName;
    private final long userId;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String status;
    private final LocalDateTime cancelledAt;

    public StudyRoomReservationView(long id, long roomId, String roomName,
                                    long userId, LocalDateTime startAt,
                                    LocalDateTime endAt, String status,
                                    LocalDateTime cancelledAt) {
        this.id = id;
        this.roomId = roomId;
        this.roomName = roomName;
        this.userId = userId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public long getId() { return id; }
    public long getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public long getUserId() { return userId; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public String getStatus() { return status; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
