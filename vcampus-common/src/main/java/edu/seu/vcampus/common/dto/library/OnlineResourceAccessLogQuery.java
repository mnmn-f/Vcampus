package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 图书管理员访问日志筛选；时间边界均为闭区间。 */
public final class OnlineResourceAccessLogQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final Long resourceId;
    private final Long userId;
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final int page;
    private final int pageSize;

    public OnlineResourceAccessLogQuery() {
        this(null, null, null, null, 1, DEFAULT_PAGE_SIZE);
    }

    public OnlineResourceAccessLogQuery(Long resourceId, Long userId,
                                        LocalDateTime from, LocalDateTime to,
                                        int page, int pageSize) {
        if (resourceId != null && resourceId.longValue() <= 0L) throw invalid();
        if (userId != null && userId.longValue() <= 0L) throw invalid();
        if (from != null && to != null && from.isAfter(to)) throw invalid();
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) throw invalid();
        this.resourceId = resourceId;
        this.userId = userId;
        this.from = from;
        this.to = to;
        this.page = page;
        this.pageSize = pageSize;
    }

    public Long getResourceId() { return resourceId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getFrom() { return from; }
    public LocalDateTime getTo() { return to; }
    public LocalDateTime getFromAccessedAt() { return from; }
    public LocalDateTime getToAccessedAt() { return to; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("访问日志筛选参数不正确");
    }
}
