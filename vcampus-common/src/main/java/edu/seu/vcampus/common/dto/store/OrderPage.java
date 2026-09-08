package edu.seu.vcampus.common.dto.store;

import java.util.List;

/** 订单分页结果。 */
public final class OrderPage extends StorePage<OrderDto> {
    private static final long serialVersionUID = 1L;

    public OrderPage(List<OrderDto> items, int page, int pageSize, long total) {
        super(items, page, pageSize, total);
    }

    public OrderPage(int page, int pageSize, long total, List<OrderDto> items) {
        this(items, page, pageSize, total);
    }

    public List<OrderDto> getOrders() { return getItems(); }
}
