package edu.seu.vcampus.client.view.modules.real;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 将各业务模块的分页 DTO 适配到 Swing 公共表格。 */
public final class PageSlice<T> {
    private final List<T> items;
    private final long total;
    private final int page;
    private final int pageSize;

    public PageSlice(List<T> items, long total, int page, int pageSize) {
        this.items = items == null ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(items));
        this.total = Math.max(0L, total);
        this.page = Math.max(1, page);
        this.pageSize = Math.max(1, pageSize);
    }

    public List<T> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public boolean hasNext() { return ((long) page * pageSize) < total; }
    public static <T> PageSlice<T> filter(List<T> source, String keyword, int page, int size,
            java.util.function.Function<T, String> text) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        List<T> matches = new ArrayList<>();
        if (source != null) for (T item : source) if (item != null
                && (query.isEmpty() || String.valueOf(text.apply(item)).toLowerCase(java.util.Locale.ROOT).contains(query))) matches.add(item);
        int actualSize = Math.max(1, size), actualPage = Math.max(1, page);
        int start = (int) Math.min(matches.size(), (long) (actualPage - 1) * actualSize);
        return new PageSlice<>(matches.subList(start, Math.min(matches.size(), start + actualSize)), matches.size(), actualPage, actualSize);
    }
}
