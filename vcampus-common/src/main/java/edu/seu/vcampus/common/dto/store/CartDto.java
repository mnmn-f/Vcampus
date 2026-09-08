package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 当前用户的活动购物车。 */
public final class CartDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long userId;
    private final String status;
    private final List<CartItemDto> items;
    private final BigDecimal totalAmount;

    public CartDto(long id, long userId, String status, List<CartItemDto> items,
                   BigDecimal totalAmount) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.items = Collections.unmodifiableList(new ArrayList<CartItemDto>(
                items == null ? Collections.<CartItemDto>emptyList() : items));
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getStatus() { return status; }
    public List<CartItemDto> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getTotal() { return totalAmount; }
}
