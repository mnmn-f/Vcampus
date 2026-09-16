package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;

/** 学生成绩导出条件；服务端仍以会话用户作为唯一学生范围。 */
public final class StudentGradeExportQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_ROWS = 5000;
    private final String semesterCode;
    private final Long courseId;
    private final String courseKeyword;

    public StudentGradeExportQuery(String semesterCode, Long courseId) {
        this(semesterCode, courseId, null);
    }

    private StudentGradeExportQuery(String semesterCode, Long courseId, String courseKeyword) {
        this.semesterCode = clean(semesterCode);
        this.courseId = courseId;
        this.courseKeyword = clean(courseKeyword);
    }

    public String getSemesterCode() { return semesterCode; }
    public Long getCourseId() { return courseId; }
    public String getCourseKeyword() { return courseKeyword; }

    public static StudentGradeExportQuery search(String semesterCode, String courseKeyword) {
        return new StudentGradeExportQuery(semesterCode, null, courseKeyword);
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
