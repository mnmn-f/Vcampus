package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;

/** 学籍管理员的分页、多条件查询条件。 */
public final class StudentProfileQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_PAGE = 1000000;

    private final String studentNo;
    private final String displayName;
    private final String college;
    private final String major;
    private final String className;
    private final StudentStatus status;
    private final int page;
    private final int pageSize;

    public StudentProfileQuery(String studentNo, String displayName,
                               String college, String major, String className,
                               StudentStatus status, int page, int pageSize) {
        this.studentNo = clean(studentNo);
        this.displayName = clean(displayName);
        this.college = clean(college);
        this.major = clean(major);
        this.className = clean(className);
        this.status = status;
        this.page = page < 1 ? 1 : Math.min(page, MAX_PAGE);
        this.pageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static StudentProfileQuery firstPage() {
        return new StudentProfileQuery(null, null, null, null, null,
                null, 1, DEFAULT_PAGE_SIZE);
    }

    public String getStudentNo() { return studentNo; }
    public String getDisplayName() { return displayName; }
    public String getCollege() { return college; }
    public String getMajor() { return major; }
    public String getClassName() { return className; }
    public StudentStatus getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
