package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import java.math.BigDecimal;

/** 学生本人某学期或累计成绩指标。空指标使用 null 数值表示暂无可计算成绩。 */
public final class StudentGradeMetricsDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String semesterCode;
    private final int courseCount;
    private final int includedCourseCount;
    private final BigDecimal credits;
    private final BigDecimal weightedGpa;
    private final BigDecimal averageGpa;
    private final BigDecimal weightedAverageScore;
    private final BigDecimal averageScore;

    public StudentGradeMetricsDto(String semesterCode, int courseCount,
                                  int includedCourseCount, BigDecimal credits,
                                  BigDecimal weightedGpa, BigDecimal averageGpa,
                                  BigDecimal weightedAverageScore,
                                  BigDecimal averageScore) {
        this.semesterCode = semesterCode;
        this.courseCount = courseCount;
        this.includedCourseCount = includedCourseCount;
        this.credits = credits;
        this.weightedGpa = weightedGpa;
        this.averageGpa = averageGpa;
        this.weightedAverageScore = weightedAverageScore;
        this.averageScore = averageScore;
    }

    public String getSemesterCode() { return semesterCode; }
    public int getCourseCount() { return courseCount; }
    public int getIncludedCourseCount() { return includedCourseCount; }
    public BigDecimal getCredits() { return credits; }
    public BigDecimal getWeightedGpa() { return weightedGpa; }
    public BigDecimal getAverageGpa() { return averageGpa; }
    public BigDecimal getWeightedAverageScore() { return weightedAverageScore; }
    public BigDecimal getAverageScore() { return averageScore; }
}
