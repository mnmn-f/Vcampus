package edu.seu.vcampus.server.dorm.repository;

import org.threeten.bp.LocalDateTime;

/** 未归扫描的输入：一名在住学生的房间与最近一次进出时间。 */
public final class ResidentAbsenceSnapshot {
    private final long studentUserId;
    private final long roomId;
    private final LocalDateTime lastExitAt;
    private final LocalDateTime lastEntryAt;

    public ResidentAbsenceSnapshot(long studentUserId, long roomId,
                                   LocalDateTime lastExitAt, LocalDateTime lastEntryAt) {
        this.studentUserId = studentUserId;
        this.roomId = roomId;
        this.lastExitAt = lastExitAt;
        this.lastEntryAt = lastEntryAt;
    }

    public long getStudentUserId() { return studentUserId; }
    public long getRoomId() { return roomId; }
    /** 最近一次离宿时间，从未离宿为 null。 */
    public LocalDateTime getLastExitAt() { return lastExitAt; }
    /** 最近一次归宿时间，从未归宿为 null。 */
    public LocalDateTime getLastEntryAt() { return lastEntryAt; }
}
