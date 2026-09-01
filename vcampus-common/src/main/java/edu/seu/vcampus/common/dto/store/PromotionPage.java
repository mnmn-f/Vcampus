package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 促销规则分页结果。 */
public final class PromotionPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<PromotionDto> items;
    private final long total;
    public PromotionPage(List<PromotionDto> items, long total) {
        this.items = Collections.unmodifiableList(new ArrayList<PromotionDto>(
                items == null ? Collections.<PromotionDto>emptyList() : items));
        this.total = total;
    }
    public List<PromotionDto> getItems() { return items; }
    public long getTotal() { return total; }
}
