package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.academic.GpaScale;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeMetricsDto;

import java.math.BigDecimal;
import java.util.List;

/** 在服务端依据成绩、学分和可信计入标记计算四项成绩指标。 */
final class StudentGradeMetricsCalculator {
    private StudentGradeMetricsCalculator() {
    }

    static StudentGradeMetricsDto calculate(String semesterCode,
                                            List<StudentGradeDto> grades) {
        int courseCount = 0;
        int includedCount = 0;
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal weightedPoints = BigDecimal.ZERO;
        BigDecimal weightedScores = BigDecimal.ZERO;
        BigDecimal points = BigDecimal.ZERO;
        BigDecimal scores = BigDecimal.ZERO;
        BigDecimal scoredCredits = BigDecimal.ZERO;
        for (StudentGradeDto grade : grades) {
            if (grade == null || grade.getScore() == null) {
                continue;
            }
            courseCount++;
            if (!grade.isGpaIncluded()) {
                continue;
            }
            BigDecimal point = GpaScale.point(grade.getScore());
            includedCount++;
            points = points.add(point);
            scores = scores.add(grade.getScore());
            BigDecimal credit = grade.getCredits();
            if (credit != null && credit.signum() > 0) {
                credits = credits.add(credit);
                scoredCredits = scoredCredits.add(credit);
                weightedPoints = weightedPoints.add(credit.multiply(point));
                weightedScores = weightedScores.add(credit.multiply(grade.getScore()));
            }
        }
        if (includedCount == 0) {
            return new StudentGradeMetricsDto(semesterCode, courseCount, 0,
                    BigDecimal.ZERO.setScale(2), null, null, null, null);
        }
        BigDecimal weightedGpa = scoredCredits.signum() == 0 ? null
                : GpaScale.round(weightedPoints.divide(scoredCredits, 8,
                BigDecimal.ROUND_HALF_UP));
        BigDecimal weightedScore = scoredCredits.signum() == 0 ? null
                : GpaScale.round(weightedScores.divide(scoredCredits, 8,
                BigDecimal.ROUND_HALF_UP));
        return new StudentGradeMetricsDto(semesterCode, courseCount, includedCount,
                GpaScale.round(credits), weightedGpa,
                GpaScale.round(points.divide(BigDecimal.valueOf(includedCount), 8,
                        BigDecimal.ROUND_HALF_UP)), weightedScore,
                GpaScale.round(scores.divide(BigDecimal.valueOf(includedCount), 8,
                        BigDecimal.ROUND_HALF_UP)));
    }
}
