package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 门禁记录视图。 */
public final class AccessRecordDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long studentUserId;
    private final String recordType;
    private final LocalDateTime occurredAt;
    private final String doorName;
    private final String source;
    private final String note;

    public AccessRecordDto(long id, long studentUserId, String recordType,
                           LocalDateTime occurredAt, String doorName,
                           String source, String note) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.recordType = recordType;
        this.occurredAt = occurredAt;
        this.doorName = doorName;
        this.source = source;
        this.note = note;
    }

    public long getId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public String getRecordType() { return recordType; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getDoorName() { return doorName; }
    public String getSource() { return source; }
    public String getNote() { return note; }
}
