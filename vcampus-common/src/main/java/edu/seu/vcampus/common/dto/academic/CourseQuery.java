package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.Locale;

/** 课程分页查询条件。空条件表示查询默认可见课程。 */
public final class CourseQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final int pageNumber;
    private final int pageSize;
    private final String keyword;
    private final String courseCode;
    private final String courseName;
    private final String status;
    private final String courseType;

    public CourseQuery() {
        this(1, DEFAULT_PAGE_SIZE, null, null, null);
    }

    public CourseQuery(int pageNumber, int pageSize, String keyword,
                       String status, String courseType) {
        this(pageNumber, pageSize, keyword, null, null, status, courseType);
    }

    public CourseQuery(int pageNumber, int pageSize, String keyword,
                       String courseCode, String courseName, String status,
                       String courseType) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize is out of range");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.keyword = textOrNull(keyword);
        this.courseCode = textOrNull(courseCode);
        this.courseName = textOrNull(courseName);
        this.status = codeOrNull(status);
        this.courseType = codeOrNull(courseType);
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getCourseCode() { return courseCode; }

    public String getCourseName() { return courseName; }

    public String getStatus() {
        return status;
    }

    public String getCourseType() {
        return courseType;
    }

    public CourseQuery withStatus(String value) {
        return new CourseQuery(pageNumber, pageSize, keyword, courseCode, courseName,
                value, courseType);
    }

    public CourseQuery withKeyword(String value) {
        return new CourseQuery(pageNumber, pageSize, value, courseCode, courseName,
                status, courseType);
    }

    private static String textOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String codeOrNull(String value) {
        String text = textOrNull(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }
}
