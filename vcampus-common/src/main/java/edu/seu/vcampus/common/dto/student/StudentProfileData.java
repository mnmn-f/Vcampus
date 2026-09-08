package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 学籍读写对象共同的业务字段，集中维护字段访问器避免 DTO 重复。 */
public abstract class StudentProfileData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long userId;
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

    protected StudentProfileData(long userId, String studentNo, String college,
                                 String major, String className, Integer enrollmentYear,
                                 Integer expectedGraduationYear, String degreeLevel,
                                 String gender, LocalDate birthDate, String address,
                                 String emergencyContact, String emergencyPhone,
                                 StudentStatus status) {
        this.userId = userId;
        this.studentNo = studentNo;
        this.college = college;
        this.major = major;
        this.className = className;
        this.enrollmentYear = enrollmentYear;
        this.expectedGraduationYear = expectedGraduationYear;
        this.degreeLevel = degreeLevel;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.emergencyContact = emergencyContact;
        this.emergencyPhone = emergencyPhone;
        this.status = status;
    }

    public final long getUserId() { return userId; }
    public final String getStudentNo() { return studentNo; }
    public final String getCollege() { return college; }
    public final String getMajor() { return major; }
    public final String getClassName() { return className; }
    public final Integer getEnrollmentYear() { return enrollmentYear; }
    public final Integer getExpectedGraduationYear() { return expectedGraduationYear; }
    public final String getDegreeLevel() { return degreeLevel; }
    public final String getGender() { return gender; }
    public final LocalDate getBirthDate() { return birthDate; }
    public final String getAddress() { return address; }
    public final String getEmergencyContact() { return emergencyContact; }
    public final String getEmergencyPhone() { return emergencyPhone; }
    public final StudentStatus getStatus() { return status; }
}
