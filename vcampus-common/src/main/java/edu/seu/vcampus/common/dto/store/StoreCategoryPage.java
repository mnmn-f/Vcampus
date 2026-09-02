package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 分页分类结果。 */
public final class StoreCategoryPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<StoreCategoryDto> items;
    private final long total;

    public StoreCategoryPage(List<StoreCategoryDto> items, long total) {
        this.items = Collections.unmodifiableList(new ArrayList<StoreCategoryDto>(
                items == null ? Collections.<StoreCategoryDto>emptyList() : items));
        this.total = total;
    }
    public List<StoreCategoryDto> getItems() { return items; }
    public long getTotal() { return total; }
}
