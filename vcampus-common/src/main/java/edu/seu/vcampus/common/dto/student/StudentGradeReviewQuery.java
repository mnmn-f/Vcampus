package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;

/** 学籍管理员核对成绩档案的筛选条件。 */
public final class StudentGradeReviewQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long studentUserId;
    private final Long courseId;
    private final int page;
    private final int pageSize;

    public StudentGradeReviewQuery(Long studentUserId, Long courseId,
                                   int page, int pageSize) {
        this.studentUserId = studentUserId;
        this.courseId = courseId;
        this.page = page < 1 ? 1 : Math.min(page, StudentProfileQuery.MAX_PAGE);
        this.pageSize = pageSize < 1 ? StudentProfileQuery.DEFAULT_PAGE_SIZE
                : Math.min(pageSize, StudentProfileQuery.MAX_PAGE_SIZE);
    }

    public static StudentGradeReviewQuery firstPage() {
        return new StudentGradeReviewQuery(null, null, 1,
                StudentProfileQuery.DEFAULT_PAGE_SIZE);
    }

    public Long getStudentUserId() { return studentUserId; }
    public Long getCourseId() { return courseId; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }
}
