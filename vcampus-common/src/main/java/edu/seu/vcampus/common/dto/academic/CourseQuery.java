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
    private final String status;
    private final String courseType;

    public CourseQuery() {
        this(1, DEFAULT_PAGE_SIZE, null, null, null);
    }

    public CourseQuery(int pageNumber, int pageSize, String keyword,
                       String status, String courseType) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize is out of range");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.keyword = textOrNull(keyword);
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

    public String getStatus() {
        return status;
    }

    public String getCourseType() {
        return courseType;
    }

    public CourseQuery withStatus(String value) {
        return new CourseQuery(pageNumber, pageSize, keyword, value, courseType);
    }

    public CourseQuery withKeyword(String value) {
        return new CourseQuery(pageNumber, pageSize, value, status, courseType);
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
