package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;

/** 学生本人查看成绩的过滤条件；服务端忽略任何客户端身份字段。 */
public final class StudentGradeQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long courseId;
    private final int page;
    private final int pageSize;

    public StudentGradeQuery(Long courseId, int page, int pageSize) {
        this.courseId = courseId;
        this.page = page < 1 ? 1 : Math.min(page, StudentProfileQuery.MAX_PAGE);
        this.pageSize = pageSize < 1 ? StudentProfileQuery.DEFAULT_PAGE_SIZE
                : Math.min(pageSize, StudentProfileQuery.MAX_PAGE_SIZE);
    }

    public static StudentGradeQuery firstPage() {
        return new StudentGradeQuery(null, 1, StudentProfileQuery.DEFAULT_PAGE_SIZE);
    }

    public Long getCourseId() { return courseId; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }
}
