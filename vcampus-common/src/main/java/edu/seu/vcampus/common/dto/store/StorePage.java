package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 商店模块统一分页结果，保证各列表接口使用同一分页语义。 */
public class StorePage<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;

    public StorePage(List<T> items, int page, int pageSize, long total) {
        if (page < 1 || pageSize < 1 || total < 0) {
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
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public long getTotalElements() { return total; }
    public boolean hasNext() { return (long) page * pageSize < total; }
    public int getTotalPages() {
        return total == 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize);
    }
}
