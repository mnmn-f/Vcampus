package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 选课关系摘要。 */
public final class EnrollmentDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long studentUserId;
    private final long courseId;
    private final String status;
    private final LocalDateTime enrolledAt;
    private final LocalDateTime droppedAt;

    public EnrollmentDto(long id, long studentUserId, long courseId, String status,
                         LocalDateTime enrolledAt, LocalDateTime droppedAt) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.courseId = courseId;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.droppedAt = droppedAt;
    }

    public long getId() { return id; }
    public long getEnrollmentId() { return id; }
    public long getStudentId() { return studentUserId; }
    public long getStudentUserId() { return studentUserId; }
    public long getCourseId() { return courseId; }
    public String getStatus() { return status; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public LocalDateTime getDroppedAt() { return droppedAt; }
}
