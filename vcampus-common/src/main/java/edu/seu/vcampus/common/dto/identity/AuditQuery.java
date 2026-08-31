package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 登录/业务审计分页条件。 */
public final class AuditQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final Long userId;
    private final String action;
    private final String outcome;
    private final int page;
    private final int pageSize;

    public AuditQuery() { this(null, null, null, 1, DEFAULT_PAGE_SIZE); }

    public AuditQuery(Long userId, String action, String outcome, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.userId = userId;
        this.action = text(action);
        this.outcome = text(outcome);
        this.page = page;
        this.pageSize = pageSize;
    }

    public AuditQuery(int page, int pageSize) {
        this(null, null, null, page, pageSize);
    }

    public Long getUserId() { return userId; }
    public String getAction() { return action; }
    public String getOutcome() { return outcome; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
