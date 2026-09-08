package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;

/** 学生成绩分页和服务端计算指标的组合结果。 */
public final class StudentGradeReportDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final StudentGradePage grades;
    private final StudentGradeMetricsDto metrics;

    public StudentGradeReportDto(StudentGradePage grades,
                                 StudentGradeMetricsDto metrics) {
        this.grades = grades;
        this.metrics = metrics;
    }

    public StudentGradePage getGrades() { return grades; }
    public StudentGradeMetricsDto getMetrics() { return metrics; }
}
