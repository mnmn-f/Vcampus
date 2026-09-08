package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** V1/V2 srtp_records 表对应的项目记录；项目参与人即 student_user_id。 */
public final class SrtpRecordDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String projectCode;
    private final long studentUserId;
    private final String title;
    private final String description;
    private final BigDecimal credits;
    private final String status;
    private final LocalDateTime submittedAt;
    private final Long reviewedBy;
    private final LocalDateTime reviewedAt;
    private final String reviewRemark;

    public SrtpRecordDto(long id, String projectCode, long studentUserId, String title,
                         String description, BigDecimal credits, String status,
                         LocalDateTime submittedAt, Long reviewedBy,
                         LocalDateTime reviewedAt, String reviewRemark) {
        this.id = id;
        this.projectCode = projectCode;
        this.studentUserId = studentUserId;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewRemark = reviewRemark;
    }

    public long getId() { return id; }
    public long getRecordId() { return id; }
    public String getProjectCode() { return projectCode; }
    public long getStudentUserId() { return studentUserId; }
    public long getStudentId() { return studentUserId; }
    public long getOwnerId() { return studentUserId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getCredits() { return credits; }
    public String getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public Long getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewRemark() { return reviewRemark; }
}
