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
    /** 申请人姓名；旧数据源没有时为 null，界面退回显示用户号。 */
    private final String studentName;
    /** 申请人当前住处（楼 房-床），入住申请或已退宿时为 null。 */
    private final String currentLocation;

    public AccommodationRequestDto(long id, long studentUserId, String requestType,
                                   Long currentRecordId, Long requestedBedId, String reason,
                                   String status, Long reviewedBy, LocalDateTime reviewedAt,
                                   String reviewRemark, LocalDateTime createdAt) {
        this(id, studentUserId, requestType, currentRecordId, requestedBedId, reason, status,
                reviewedBy, reviewedAt, reviewRemark, createdAt, null, null);
    }

    public AccommodationRequestDto(long id, long studentUserId, String requestType,
                                   Long currentRecordId, Long requestedBedId, String reason,
                                   String status, Long reviewedBy, LocalDateTime reviewedAt,
                                   String reviewRemark, LocalDateTime createdAt,
                                   String studentName, String currentLocation) {
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
        this.studentName = studentName;
        this.currentLocation = currentLocation;
    }

    public long getId() { return id; }
    public long getRequestId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public String getStudentName() { return studentName; }
    public String getCurrentLocation() { return currentLocation; }
    /** 界面用：有姓名显示姓名，没有就退回「用户 N」。 */
    public String studentLabel() {
        return studentName == null || studentName.trim().isEmpty() ? "用户 " + studentUserId : studentName.trim();
    }
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
