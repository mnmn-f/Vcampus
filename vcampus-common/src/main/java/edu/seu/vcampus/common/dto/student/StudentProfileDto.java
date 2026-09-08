package edu.seu.vcampus.common.dto.student;

import org.threeten.bp.LocalDate;

/** 学生学籍档案的跨端只读数据。 */
public final class StudentProfileDto extends StudentProfileData {
    private static final long serialVersionUID = 1L;

    private final String displayName;
    private final String account;

    public StudentProfileDto(long userId, String displayName, String account,
                             String studentNo, String college, String major,
                             String className, Integer enrollmentYear,
                             Integer expectedGraduationYear, String degreeLevel,
                             String gender, LocalDate birthDate, String address,
                             String emergencyContact, String emergencyPhone,
                             StudentStatus status) {
        super(userId, studentNo, college, major, className, enrollmentYear,
                expectedGraduationYear, degreeLevel, gender, birthDate, address,
                emergencyContact, emergencyPhone, status);
        this.displayName = displayName;
        this.account = account;
    }

    public String getDisplayName() { return displayName; }
    public String getAccount() { return account; }
}
