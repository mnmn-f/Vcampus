package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 图书管理员借阅台账筛选；学生编号只在管理员命令中生效。 */
public final class BorrowAdminSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final String status;
    private final Long studentId;
    private final String keyword;
    private final int page;
    private final int pageSize;

    public BorrowAdminSearchRequest() {
        this(null, null, null, 1, DEFAULT_PAGE_SIZE);
    }

    public BorrowAdminSearchRequest(String status, Long studentId, String keyword,
                                    int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.status = text(status);
        this.studentId = studentId;
        this.keyword = text(keyword);
        this.page = page;
        this.pageSize = pageSize;
    }

    public BorrowAdminSearchRequest(String status, long studentId, String keyword,
                                    int page, int pageSize) {
        this(status, Long.valueOf(studentId), keyword, page, pageSize);
    }

    public String getStatus() { return status; }
    public Long getStudentId() { return studentId; }
    public Long getBorrowerUserId() { return studentId; }
    public String getKeyword() { return keyword; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
