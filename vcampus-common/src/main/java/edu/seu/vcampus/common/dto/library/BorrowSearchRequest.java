package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 借阅列表筛选；学生查询时服务端忽略任何用户编号字段。 */
public final class BorrowSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String status;
    private final int page;
    private final int pageSize;

    public BorrowSearchRequest(String status, int page, int pageSize) {
        this.status = status;
        this.page = page;
        this.pageSize = pageSize;
    }

    public BorrowSearchRequest() { this(null, 1, 20); }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
