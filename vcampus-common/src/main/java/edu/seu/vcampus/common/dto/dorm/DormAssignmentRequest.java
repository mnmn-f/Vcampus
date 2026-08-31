package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 宿管直接分配、调宿或退宿请求；操作人取自会话。 */
public final class DormAssignmentRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long studentUserId;
    private final Long recordId;
    private final Long bedId;
    private final LocalDate effectiveDate;
    private final String reason;

    public DormAssignmentRequest(long studentUserId, Long recordId, Long bedId,
                                 LocalDate effectiveDate, String reason) {
        this.studentUserId = studentUserId;
        this.recordId = recordId;
        this.bedId = bedId;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
    }

    public DormAssignmentRequest(long studentUserId, long bedId, LocalDate effectiveDate) {
        this(studentUserId, null, Long.valueOf(bedId), effectiveDate, null);
    }

    public long getStudentUserId() { return studentUserId; }
    public Long getRecordId() { return recordId; }
    public Long getBedId() { return bedId; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getReason() { return reason; }
}
