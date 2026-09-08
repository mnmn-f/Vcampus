package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 课程列表和详情共用的传输对象。 */
public final class CourseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String courseCode;
    private final String courseName;
    private final String courseType;
    private final String semesterCode;
    private final BigDecimal credits;
    private final Integer totalHours;
    private final int capacity;
    private final long enrolledCount;
    private final String description;
    private final String status;
    private final List<CourseScheduleDto> schedules;
    private final List<CourseInstructorDto> instructors;

    public CourseDto(long id, String courseCode, String courseName, String courseType,
                     BigDecimal credits, Integer totalHours, int capacity,
                     long enrolledCount, String description, String status,
                     List<CourseScheduleDto> schedules,
                     List<CourseInstructorDto> instructors) {
        this(id, courseCode, courseName, courseType, credits, totalHours, capacity,
                enrolledCount, description, status, schedules, instructors, null);
    }

    public CourseDto(long id, String courseCode, String courseName, String courseType,
                     BigDecimal credits, Integer totalHours, int capacity,
                     long enrolledCount, String description, String status,
                     List<CourseScheduleDto> schedules,
                     List<CourseInstructorDto> instructors, String semesterCode) {
        this.id = id;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.courseType = courseType;
        this.semesterCode = semesterCode;
        this.credits = credits;
        this.totalHours = totalHours;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
        this.description = description;
        this.status = status;
        this.schedules = immutable(schedules);
        this.instructors = immutable(instructors);
    }

    public long getId() { return id; }
    public long getCourseId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getCourseType() { return courseType; }
    public String getSemesterCode() { return semesterCode; }
    public BigDecimal getCredits() { return credits; }
    public Integer getTotalHours() { return totalHours; }
    public int getCapacity() { return capacity; }
    public long getEnrolledCount() { return enrolledCount; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public List<CourseScheduleDto> getSchedules() { return schedules; }
    public List<CourseInstructorDto> getInstructors() { return instructors; }

    private static <T> List<T> immutable(List<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
