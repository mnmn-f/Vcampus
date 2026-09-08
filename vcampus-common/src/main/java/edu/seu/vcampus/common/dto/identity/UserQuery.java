package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 系统管理员用户分页检索条件。 */
public final class UserQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final String keyword;
    private final String status;
    private final int page;
    private final int pageSize;

    public UserQuery() { this(null, null, 1, DEFAULT_PAGE_SIZE); }

    public UserQuery(String keyword, String status, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.keyword = text(keyword);
        this.status = text(status);
        this.page = page;
        this.pageSize = pageSize;
    }

    public UserQuery(int page, int pageSize, String keyword, String status) {
        this(keyword, status, page, pageSize);
    }

    public String getKeyword() { return keyword; }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
