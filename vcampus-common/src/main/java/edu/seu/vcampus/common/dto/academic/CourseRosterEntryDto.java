package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 课程花名册中的只读学生与选课摘要。 */
public final class CourseRosterEntryDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long enrollmentId;
    private final long studentUserId;
    private final String studentNo;
    private final String displayName;
    private final String college;
    private final String major;
    private final String className;
    private final String enrollmentStatus;
    private final LocalDateTime enrolledAt;
    private final BigDecimal score;
    private final String gradeRemark;

    public CourseRosterEntryDto(long enrollmentId, long studentUserId, String studentNo,
                                String displayName, String college, String major,
                                String className, String enrollmentStatus,
                                LocalDateTime enrolledAt) {
        this(enrollmentId, studentUserId, studentNo, displayName, college, major,
                className, enrollmentStatus, enrolledAt, null, null);
    }

    public CourseRosterEntryDto(long enrollmentId, long studentUserId, String studentNo,
                                String displayName, String college, String major,
                                String className, String enrollmentStatus,
                                LocalDateTime enrolledAt, BigDecimal score,
                                String gradeRemark) {
        this.enrollmentId = enrollmentId;
        this.studentUserId = studentUserId;
        this.studentNo = studentNo;
        this.displayName = displayName;
        this.college = college;
        this.major = major;
        this.className = className;
        this.enrollmentStatus = enrollmentStatus;
        this.enrolledAt = enrolledAt;
        this.score = score;
        this.gradeRemark = gradeRemark;
    }

    public long getEnrollmentId() { return enrollmentId; }
    public long getStudentUserId() { return studentUserId; }
    public String getStudentNo() { return studentNo; }
    public String getDisplayName() { return displayName; }
    public String getCollege() { return college; }
    public String getMajor() { return major; }
    public String getClassName() { return className; }
    public String getEnrollmentStatus() { return enrollmentStatus; }
    public String getStatus() { return enrollmentStatus; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public BigDecimal getScore() { return score; }
    public String getGradeRemark() { return gradeRemark; }
}
