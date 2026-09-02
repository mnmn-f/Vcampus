package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 学生成绩的跨端只读数据。 */
public final class StudentGradeDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long gradeId;
    private final long enrollmentId;
    private final long studentUserId;
    private final long courseId;
    private final String courseCode;
    private final String courseName;
    private final BigDecimal score;
    private final BigDecimal gradePoint;
    private final long recordedBy;
    private final LocalDateTime recordedAt;
    private final String remark;
    private final String enrollmentStatus;

    public StudentGradeDto(long gradeId, long enrollmentId, long studentUserId,
                           long courseId, String courseCode, String courseName,
                           BigDecimal score, BigDecimal gradePoint,
                           long recordedBy, LocalDateTime recordedAt,
                           String remark, String enrollmentStatus) {
        this.gradeId = gradeId;
        this.enrollmentId = enrollmentId;
        this.studentUserId = studentUserId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.score = score;
        this.gradePoint = gradePoint;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
        this.remark = remark;
        this.enrollmentStatus = enrollmentStatus;
    }

    public long getGradeId() { return gradeId; }
    public long getEnrollmentId() { return enrollmentId; }
    public long getStudentUserId() { return studentUserId; }
    public long getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public BigDecimal getScore() { return score; }
    public BigDecimal getGradePoint() { return gradePoint; }
    public long getRecordedBy() { return recordedBy; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public String getRemark() { return remark; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
}
