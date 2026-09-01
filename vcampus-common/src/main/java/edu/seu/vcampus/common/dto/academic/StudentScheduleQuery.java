package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 学生本人课表条件；空学期表示累计展示所有有效选课。 */
public final class StudentScheduleQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String semesterCode;

    public StudentScheduleQuery(String semesterCode) {
        this.semesterCode = clean(semesterCode);
    }

    public static StudentScheduleQuery all() {
        return new StudentScheduleQuery(null);
    }

    public String getSemesterCode() { return semesterCode; }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
