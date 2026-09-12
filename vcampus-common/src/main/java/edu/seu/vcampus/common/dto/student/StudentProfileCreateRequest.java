package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 学籍管理员从待建档账号创建档案；以账号解析目标，不接收内部用户主键。 */
public final class StudentProfileCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String account;
    private final String studentNo;
    private final String college;
    private final String major;
    private final String className;
    private final Integer enrollmentYear;
    private final Integer expectedGraduationYear;
    private final String degreeLevel;
    private final String gender;
    private final LocalDate birthDate;
    private final String address;
    private final String emergencyContact;
    private final String emergencyPhone;
    private final StudentStatus status;

    public StudentProfileCreateRequest(String account, String studentNo, String college,
                                       String major, String className, Integer enrollmentYear,
                                       Integer expectedGraduationYear, String degreeLevel,
                                       String gender, LocalDate birthDate, String address,
                                       String emergencyContact, String emergencyPhone,
                                       StudentStatus status) {
        this.account = clean(account); this.studentNo = clean(studentNo);
        this.college = clean(college); this.major = clean(major);
        this.className = clean(className); this.enrollmentYear = enrollmentYear;
        this.expectedGraduationYear = expectedGraduationYear;
        this.degreeLevel = clean(degreeLevel); this.gender = clean(gender);
        this.birthDate = birthDate; this.address = clean(address);
        this.emergencyContact = clean(emergencyContact);
        this.emergencyPhone = clean(emergencyPhone); this.status = status;
    }

    public String getAccount() { return account; }
    public String getStudentNo() { return studentNo; }
    public String getCollege() { return college; }
    public String getMajor() { return major; }
    public String getClassName() { return className; }
    public Integer getEnrollmentYear() { return enrollmentYear; }
    public Integer getExpectedGraduationYear() { return expectedGraduationYear; }
    public String getDegreeLevel() { return degreeLevel; }
    public String getGender() { return gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getAddress() { return address; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public StudentStatus getStatus() { return status; }

    public StudentProfileWriteRequest withUserId(long userId) {
        return new StudentProfileWriteRequest(userId, studentNo, college, major, className,
                enrollmentYear, expectedGraduationYear, degreeLevel, gender, birthDate,
                address, emergencyContact, emergencyPhone, status);
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
