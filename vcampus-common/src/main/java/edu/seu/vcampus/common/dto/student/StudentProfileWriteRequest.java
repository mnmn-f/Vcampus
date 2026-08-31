package edu.seu.vcampus.common.dto.student;

import org.threeten.bp.LocalDate;

/** 学籍管理员新增或修改档案的请求；userId只表示管理目标，不能作为操作者身份。 */
public final class StudentProfileWriteRequest extends StudentProfileData {
    private static final long serialVersionUID = 1L;

    public StudentProfileWriteRequest(long userId, String studentNo,
                                      String college, String major,
                                      String className, Integer enrollmentYear,
                                      Integer expectedGraduationYear,
                                      String degreeLevel, String gender,
                                      LocalDate birthDate, String address,
                                      String emergencyContact,
                                      String emergencyPhone, StudentStatus status) {
        super(userId, clean(studentNo), clean(college), clean(major), clean(className),
                enrollmentYear, expectedGraduationYear, clean(degreeLevel), clean(gender),
                birthDate, clean(address), clean(emergencyContact), clean(emergencyPhone),
                status);
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
