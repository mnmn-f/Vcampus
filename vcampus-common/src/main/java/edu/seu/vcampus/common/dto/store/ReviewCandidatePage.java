package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 待评价订单商品分页，避免客户端截断历史订单。 */
public final class ReviewCandidatePage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<ReviewCandidateDto> items;
    private final long total;
    public ReviewCandidatePage(List<ReviewCandidateDto> items, long total) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items)); this.total = total;
    }
    public List<ReviewCandidateDto> getItems() { return items; }
    public long getTotal() { return total; }
}
