package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 门禁进出登记；学生身份由 SessionContext 决定。 */
public final class AccessRecordRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String recordType;
    private final LocalDateTime occurredAt;
    private final String doorName;
    private final String source;
    private final String note;

    public AccessRecordRequest(String recordType, LocalDateTime occurredAt,
                               String doorName, String source, String note) {
        this.recordType = recordType;
        this.occurredAt = occurredAt;
        this.doorName = doorName;
        this.source = source;
        this.note = note;
    }

    public String getRecordType() { return recordType; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getDoorName() { return doorName; }
    public String getSource() { return source; }
    public String getNote() { return note; }
}
