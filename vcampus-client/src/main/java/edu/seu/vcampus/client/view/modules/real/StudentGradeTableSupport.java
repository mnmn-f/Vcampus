package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;

/** 学生成绩表在不同入口共用的分页和行映射。 */
final class StudentGradeTableSupport {
    private StudentGradeTableSupport() { }

    static PageSlice<StudentGradeDto> slice(StudentGradePage page) {
        return new PageSlice<StudentGradeDto>(page.getItems(), page.getTotal(),
                page.getPage(), page.getPageSize());
    }

    static Object[] row(StudentGradeDto grade) {
        return new Object[] {RealUi.text(grade.getSemesterCode()), grade.getCourseCode(),
                grade.getCourseName(), RealUi.text(grade.getCredits()),
                RealUi.text(grade.getScore()), RealUi.text(grade.getGradePoint()),
                grade.isGpaIncluded() ? RealUi.status(grade.getEnrollmentStatus()) : "不计入指标"};
    }
}
