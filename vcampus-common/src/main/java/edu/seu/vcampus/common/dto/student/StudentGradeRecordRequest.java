package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import java.math.BigDecimal;

/** 任课教师登记本人课程成绩的请求。 */
public final class StudentGradeRecordRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long enrollmentId;
    private final BigDecimal score;
    private final BigDecimal gradePoint;
    private final String remark;

    public StudentGradeRecordRequest(long enrollmentId, BigDecimal score,
                                     BigDecimal gradePoint, String remark) {
        this.enrollmentId = enrollmentId;
        this.score = score;
        this.gradePoint = gradePoint;
        this.remark = remark == null || remark.trim().isEmpty() ? null : remark.trim();
    }

    public long getEnrollmentId() { return enrollmentId; }
    public BigDecimal getScore() { return score; }
    public BigDecimal getGradePoint() { return gradePoint; }
    public String getRemark() { return remark; }
}
