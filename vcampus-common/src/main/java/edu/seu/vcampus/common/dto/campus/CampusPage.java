package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 教务扩展模块统一分页结果。 */
public final class CampusPage<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final List<T> items;

    public CampusPage(int pageNumber, int pageSize, long totalElements, List<T> items) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.items = items == null || items.isEmpty()
                ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(items));
    }

    public CampusPage(List<T> items, int pageNumber, int pageSize, long totalElements) {
        this(pageNumber, pageSize, totalElements, items);
    }

    public int getPageNumber() { return pageNumber; }
    public int getPage() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public long getTotalElements() { return totalElements; }
    public long getTotal() { return totalElements; }
    public List<T> getItems() { return items; }
    public int getTotalPages() {
        return totalElements == 0 ? 0 : (int) ((totalElements + pageSize - 1L) / pageSize);
    }
}
