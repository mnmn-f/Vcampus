package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 账号注销申请分页条件；不携带用户身份，个人范围由服务端注入。 */
public final class AccountCancellationQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final String status;
    private final int page;
    private final int pageSize;

    public AccountCancellationQuery() {
        this(null, 1, DEFAULT_PAGE_SIZE);
    }

    public AccountCancellationQuery(String status, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.status = text(status);
        this.page = page;
        this.pageSize = pageSize;
    }

    public AccountCancellationQuery(int page, int pageSize) {
        this(null, page, pageSize);
    }

    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
