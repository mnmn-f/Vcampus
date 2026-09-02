package edu.seu.vcampus.common.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** AI 管理页面使用的通用安全分页结果。 */
public final class AiPage<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<T> items;
    private final long total;
    private final int page;
    private final int pageSize;

    public AiPage(List<T> items, long total, int page, int pageSize) {
        this.items = items == null ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(items));
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<T> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
