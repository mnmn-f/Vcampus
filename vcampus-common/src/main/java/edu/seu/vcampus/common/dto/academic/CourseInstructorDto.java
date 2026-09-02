package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 课程授课教师摘要。 */
public final class CourseInstructorDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long teacherUserId;
    private final String displayName;
    private final String employeeNo;
    private final String instructorRole;

    public CourseInstructorDto(long teacherUserId, String displayName,
                               String instructorRole) {
        this(teacherUserId, displayName, null, instructorRole);
    }

    public CourseInstructorDto(long teacherUserId, String displayName,
                               String employeeNo, String instructorRole) {
        this.teacherUserId = teacherUserId;
        this.displayName = displayName;
        this.employeeNo = employeeNo;
        this.instructorRole = instructorRole;
    }

    public long getTeacherUserId() { return teacherUserId; }
    public String getDisplayName() { return displayName; }
    public String getEmployeeNo() { return employeeNo; }
    public String getInstructorRole() { return instructorRole; }
}
