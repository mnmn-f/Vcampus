package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 线上资源访问日志分页结果。 */
public final class OnlineResourceAccessLogPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<OnlineResourceAccessLogDto> items;
    private final int page;
    private final int pageSize;
    private final long total;

    public OnlineResourceAccessLogPage(List<OnlineResourceAccessLogDto> items,
                                       int page, int pageSize, long total) {
        if (page < 1 || pageSize < 1 || total < 0L) throw new IllegalArgumentException("分页参数不正确");
        this.items = Collections.unmodifiableList(new ArrayList<OnlineResourceAccessLogDto>(
                items == null ? Collections.<OnlineResourceAccessLogDto>emptyList() : items));
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public OnlineResourceAccessLogPage(int page, int pageSize, long total,
                                       List<OnlineResourceAccessLogDto> items) {
        this(items, page, pageSize, total);
    }

    public List<OnlineResourceAccessLogDto> getItems() { return items; }
    public List<OnlineResourceAccessLogDto> getLogs() { return items; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public boolean hasNext() { return ((long) page * pageSize) < total; }
}
