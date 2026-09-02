package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/**
 * 连续未归预警视图。
 *
 * <p>与 V1 的 late_return_alerts（晚归：回来了但超时）是两件事，本类描述的是
 * 「一直没回来」，因此有连续未归天数和分级。</p>
 */
public final class AbsenceWarningDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 一般：达到预警阈值。 */
    public static final String LEVEL_NORMAL = "NORMAL";
    /** 严重：达到通知阈值，应当通知辅导员。 */
    public static final String LEVEL_SEVERE = "SEVERE";
    /** 已豁免：该日有已批准的请假。 */
    public static final String LEVEL_EXEMPT = "EXEMPT";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_NOTIFIED = "NOTIFIED";
    public static final String STATUS_VERIFIED = "VERIFIED";

    private final long id;
    private final long studentUserId;
    private final long roomId;
    private final String buildingCode;
    private final String roomNo;
    private final LocalDate scanDate;
    private final LocalDateTime lastLeaveAt;
    private final int absenceDays;
    private final String warningLevel;
    private final String handleStatus;
    private final Long notifiedTeacherId;
    private final LocalDateTime notifiedAt;
    private final String note;

    public AbsenceWarningDto(long id, long studentUserId, long roomId, String buildingCode,
                             String roomNo, LocalDate scanDate, LocalDateTime lastLeaveAt,
                             int absenceDays, String warningLevel, String handleStatus,
                             Long notifiedTeacherId, LocalDateTime notifiedAt, String note) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.roomId = roomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.scanDate = scanDate;
        this.lastLeaveAt = lastLeaveAt;
        this.absenceDays = absenceDays;
        this.warningLevel = warningLevel;
        this.handleStatus = handleStatus;
        this.notifiedTeacherId = notifiedTeacherId;
        this.notifiedAt = notifiedAt;
        this.note = note;
    }

    public long getId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public long getRoomId() { return roomId; }
    public String getBuildingCode() { return buildingCode; }
    public String getRoomNo() { return roomNo; }
    public LocalDate getScanDate() { return scanDate; }
    public LocalDateTime getLastLeaveAt() { return lastLeaveAt; }
    public int getAbsenceDays() { return absenceDays; }
    public String getWarningLevel() { return warningLevel; }
    public String getHandleStatus() { return handleStatus; }
    public Long getNotifiedTeacherId() { return notifiedTeacherId; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public String getNote() { return note; }

    public boolean isSevere() { return LEVEL_SEVERE.equals(warningLevel); }
    public boolean isExempt() { return LEVEL_EXEMPT.equals(warningLevel); }
}
