package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 用户优惠券分页结果。 */
public final class CouponPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<CouponDto> items;
    private final long total;
    public CouponPage(List<CouponDto> items, long total) {
        this.items = Collections.unmodifiableList(new ArrayList<CouponDto>(
                items == null ? Collections.<CouponDto>emptyList() : items));
        this.total = total;
    }
    public List<CouponDto> getItems() { return items; }
    public long getTotal() { return total; }
}
