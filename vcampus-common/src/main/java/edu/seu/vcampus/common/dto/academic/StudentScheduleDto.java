package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 当前会话学生的个人课表。 */
public final class StudentScheduleDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long studentUserId;
    private final List<CourseDto> courses;

    public StudentScheduleDto(long studentUserId, List<CourseDto> courses) {
        this.studentUserId = studentUserId;
        this.courses = courses == null || courses.isEmpty()
                ? Collections.<CourseDto>emptyList()
                : Collections.unmodifiableList(new ArrayList<CourseDto>(courses));
    }

    public long getStudentUserId() { return studentUserId; }
    public long getStudentId() { return studentUserId; }
    public List<CourseDto> getCourses() { return courses; }
}
