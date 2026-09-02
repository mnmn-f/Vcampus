package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 带晚归判定的进出记录；晚归按当前门禁策略实时算出，不落库。 */
public final class AccessRecordExtDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long studentUserId;
    private final String recordType;
    private final LocalDateTime occurredAt;
    private final String doorName;
    private final boolean lateReturn;

    public AccessRecordExtDto(long id, long studentUserId, String recordType,
                              LocalDateTime occurredAt, String doorName, boolean lateReturn) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.recordType = recordType;
        this.occurredAt = occurredAt;
        this.doorName = doorName;
        this.lateReturn = lateReturn;
    }

    public long getId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public String getRecordType() { return recordType; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getDoorName() { return doorName; }
    /** 仅归宿记录可能为真。 */
    public boolean isLateReturn() { return lateReturn; }
}
