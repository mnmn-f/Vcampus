package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 报修工单视图。 */
public final class RepairOrderDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long roomId;
    private final long reporterId;
    private final String category;
    private final String description;
    private final String priority;
    private final String status;
    private final Long handlerId;
    private final LocalDateTime submittedAt;
    private final LocalDateTime acceptedAt;
    private final LocalDateTime completedAt;
    private final Integer evaluationScore;
    private final String evaluationNote;

    public RepairOrderDto(long id, long roomId, long reporterId, String category,
                          String description, String priority, String status,
                          Long handlerId, LocalDateTime submittedAt,
                          LocalDateTime acceptedAt, LocalDateTime completedAt,
                          Integer evaluationScore, String evaluationNote) {
        this.id = id;
        this.roomId = roomId;
        this.reporterId = reporterId;
        this.category = category;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.handlerId = handlerId;
        this.submittedAt = submittedAt;
        this.acceptedAt = acceptedAt;
        this.completedAt = completedAt;
        this.evaluationScore = evaluationScore;
        this.evaluationNote = evaluationNote;
    }

    public long getId() { return id; }
    public long getOrderId() { return id; }
    public long getRepairOrderId() { return id; }
    public long getRoomId() { return roomId; }
    public long getReporterId() { return reporterId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public Long getHandlerId() { return handlerId; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Integer getEvaluationScore() { return evaluationScore; }
    public String getEvaluationNote() { return evaluationNote; }
}
