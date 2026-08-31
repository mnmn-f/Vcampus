package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 教室申请及审批视图。 */
public final class ClassroomReservationDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long classroomId;
    private final String buildingName;
    private final String roomNo;
    private final long applicantId;
    private final String purpose;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String status;
    private final Long reviewedBy;
    private final LocalDateTime reviewedAt;
    private final String reviewRemark;

    public ClassroomReservationDto(long id, long classroomId, String buildingName,
            String roomNo, long applicantId, String purpose, LocalDateTime startAt,
            LocalDateTime endAt, String status, Long reviewedBy,
            LocalDateTime reviewedAt, String reviewRemark) {
        this.id = id;
        this.classroomId = classroomId;
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.applicantId = applicantId;
        this.purpose = purpose;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewRemark = reviewRemark;
    }

    public long getId() { return id; }
    public long getReservationId() { return id; }
    public long getClassroomId() { return classroomId; }
    public String getBuildingName() { return buildingName; }
    public String getRoomNo() { return roomNo; }
    public long getApplicantId() { return applicantId; }
    public long getApplicantUserId() { return applicantId; }
    public String getPurpose() { return purpose; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getStartTime() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public LocalDateTime getEndTime() { return endAt; }
    public String getStatus() { return status; }
    public Long getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewRemark() { return reviewRemark; }
}
