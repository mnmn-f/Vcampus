package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** 未归/异常记录视图。 */
public final class LateReturnAlertDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long studentUserId;
    private final LocalDate alertDate;
    private final LocalDateTime detectedAt;
    private final String status;
    private final Long handledBy;
    private final LocalDateTime handledAt;
    private final String note;

    public LateReturnAlertDto(long id, long studentUserId, LocalDate alertDate,
                              LocalDateTime detectedAt, String status, Long handledBy,
                              LocalDateTime handledAt, String note) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.alertDate = alertDate;
        this.detectedAt = detectedAt;
        this.status = status;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.note = note;
    }

    public long getId() { return id; }
    public long getAlertId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public LocalDate getAlertDate() { return alertDate; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public String getStatus() { return status; }
    public Long getHandledBy() { return handledBy; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public String getNote() { return note; }
}
