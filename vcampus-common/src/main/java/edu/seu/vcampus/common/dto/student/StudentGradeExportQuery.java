package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;

/** 学生成绩导出条件；服务端仍以会话用户作为唯一学生范围。 */
public final class StudentGradeExportQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_ROWS = 5000;
    private final String semesterCode;
    private final Long courseId;

    public StudentGradeExportQuery(String semesterCode, Long courseId) {
        this.semesterCode = clean(semesterCode);
        this.courseId = courseId;
    }

    public String getSemesterCode() { return semesterCode; }
    public Long getCourseId() { return courseId; }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
