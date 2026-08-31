package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 住宿申请及审批结果。 */
public final class AccommodationRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long studentUserId;
    private final String requestType;
    private final Long currentRecordId;
    private final Long requestedBedId;
    private final String reason;
    private final String status;
    private final Long reviewedBy;
    private final LocalDateTime reviewedAt;
    private final String reviewRemark;
    private final LocalDateTime createdAt;

    public AccommodationRequestDto(long id, long studentUserId, String requestType,
                                   Long currentRecordId, Long requestedBedId, String reason,
                                   String status, Long reviewedBy, LocalDateTime reviewedAt,
                                   String reviewRemark, LocalDateTime createdAt) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.requestType = requestType;
        this.currentRecordId = currentRecordId;
        this.requestedBedId = requestedBedId;
        this.reason = reason;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewRemark = reviewRemark;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getRequestId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public String getRequestType() { return requestType; }
    public Long getCurrentRecordId() { return currentRecordId; }
    public Long getRequestedBedId() { return requestedBedId; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public Long getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewRemark() { return reviewRemark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
