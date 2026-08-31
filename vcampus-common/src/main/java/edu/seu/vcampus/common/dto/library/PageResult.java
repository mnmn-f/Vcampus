package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 通用的、可传输的分页结果。 */
public final class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;

    public PageResult(List<T> items, int page, int pageSize, long total) {
        if (page <= 0 || pageSize <= 0 || total < 0) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.items = Collections.unmodifiableList(new ArrayList<T>(
                items == null ? Collections.<T>emptyList() : items));
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public boolean hasNext() { return ((long) page * pageSize) < total; }
}
