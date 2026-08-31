package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 教室申请请求；applicantId 不属于客户端可控字段。 */
public final class ClassroomReservationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long classroomId;
    private final String purpose;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;

    public ClassroomReservationRequest(long classroomId, String purpose,
                                       LocalDateTime startAt, LocalDateTime endAt) {
        this.classroomId = classroomId;
        this.purpose = purpose;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public long getClassroomId() { return classroomId; }
    public String getPurpose() { return purpose; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getStartTime() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public LocalDateTime getEndTime() { return endAt; }
}
