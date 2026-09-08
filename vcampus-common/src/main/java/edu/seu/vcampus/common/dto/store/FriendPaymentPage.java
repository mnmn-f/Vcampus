package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 好友代付分页结果。 */
public final class FriendPaymentPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<FriendPaymentDto> items;
    private final long total;
    public FriendPaymentPage(List<FriendPaymentDto> items, long total) {
        this.items = Collections.unmodifiableList(new ArrayList<FriendPaymentDto>(
                items == null ? Collections.<FriendPaymentDto>emptyList() : items));
        this.total = total;
    }
    public List<FriendPaymentDto> getItems() { return items; }
    public long getTotal() { return total; }
}
