package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 教务老师创建或修改课程的请求；授课教师关系随课程一并维护。 */
public final class CourseSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long courseId;
    private final String courseCode;
    private final String courseName;
    private final String courseType;
    private final String semesterCode;
    private final BigDecimal credits;
    private final Integer totalHours;
    private final Integer capacity;
    private final String description;
    private final String status;
    private final List<Long> instructorUserIds;

    public CourseSaveRequest(Long courseId, String courseCode, String courseName,
                             String courseType, BigDecimal credits, Integer totalHours,
                             Integer capacity, String description, String status,
                             List<Long> instructorUserIds) {
        this(courseId, courseCode, courseName, courseType, credits, totalHours, capacity,
                description, status, instructorUserIds, null);
    }

    public CourseSaveRequest(Long courseId, String courseCode, String courseName,
                             String courseType, BigDecimal credits, Integer totalHours,
                             Integer capacity, String description, String status,
                             List<Long> instructorUserIds, String semesterCode) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.courseType = codeOrNull(courseType);
        this.semesterCode = codeOrNull(semesterCode);
        this.credits = credits;
        this.totalHours = totalHours;
        this.capacity = capacity;
        this.description = description;
        this.status = codeOrNull(status);
        this.instructorUserIds = immutableIds(instructorUserIds);
    }

    public static CourseSaveRequest create(String code, String name, CourseType type,
                                           BigDecimal credits, Integer totalHours,
                                           Integer capacity, String description,
                                           CourseStatus status, List<Long> teachers) {
        return create(code, name, type, credits, totalHours, capacity, description,
                status, teachers, null);
    }

    public static CourseSaveRequest create(String code, String name, CourseType type,
                                           BigDecimal credits, Integer totalHours,
                                           Integer capacity, String description,
                                           CourseStatus status, List<Long> teachers,
                                           String semesterCode) {
        return new CourseSaveRequest(null, code, name, type == null ? null : type.name(),
                credits, totalHours, capacity, description,
                status == null ? null : status.name(), teachers, semesterCode);
    }

    public static CourseSaveRequest update(long id, String code, String name,
                                           CourseType type, BigDecimal credits,
                                           Integer totalHours, Integer capacity,
                                           String description, CourseStatus status,
                                           List<Long> teachers) {
        return update(id, code, name, type, credits, totalHours, capacity, description,
                status, teachers, null);
    }

    public static CourseSaveRequest update(long id, String code, String name,
                                           CourseType type, BigDecimal credits,
                                           Integer totalHours, Integer capacity,
                                           String description, CourseStatus status,
                                           List<Long> teachers, String semesterCode) {
        if (id <= 0) {
            throw new IllegalArgumentException("course id must be positive");
        }
        return new CourseSaveRequest(Long.valueOf(id), code, name,
                type == null ? null : type.name(), credits, totalHours, capacity,
                description, status == null ? null : status.name(), teachers, semesterCode);
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getId() {
        return courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseType() {
        return courseType;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public BigDecimal getCredits() {
        return credits;
    }

    public Integer getTotalHours() {
        return totalHours;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public List<Long> getInstructorUserIds() {
        return instructorUserIds;
    }

    public boolean isUpdate() {
        return courseId != null;
    }

    private static List<Long> immutableIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<Long>(ids));
    }

    private static String codeOrNull(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

}
