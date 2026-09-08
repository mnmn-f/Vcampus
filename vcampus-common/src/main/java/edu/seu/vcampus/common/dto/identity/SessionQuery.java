package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 系统管理员会话分页条件；userId 仅是检索条件而非操作者身份。 */
public final class SessionQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final Long userId;
    private final boolean includeRevoked;
    private final int page;
    private final int pageSize;

    public SessionQuery() { this(null, false, 1, DEFAULT_PAGE_SIZE); }

    public SessionQuery(Long userId, boolean includeRevoked, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.userId = userId;
        this.includeRevoked = includeRevoked;
        this.page = page;
        this.pageSize = pageSize;
    }

    public SessionQuery(long userId, int page, int pageSize) {
        this(Long.valueOf(userId), false, page, pageSize);
    }

    public Long getUserId() { return userId; }
    public boolean isIncludeRevoked() { return includeRevoked; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }
}
