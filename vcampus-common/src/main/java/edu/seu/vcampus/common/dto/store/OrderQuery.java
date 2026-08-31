package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 订单列表条件；本人查询中的 buyerId 由服务端忽略。 */
public final class OrderQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final String orderNo;
    private final Long buyerId;
    private final String status;
    private final int page;
    private final int pageSize;

    public OrderQuery() { this(null, null, null, 1, DEFAULT_PAGE_SIZE); }

    public OrderQuery(String orderNo, String status, int page, int pageSize) {
        this(orderNo, null, status, page, pageSize);
    }

    public OrderQuery(String orderNo, Long buyerId, String status,
                      int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.orderNo = text(orderNo);
        this.buyerId = buyerId;
        this.status = text(status);
        this.page = page;
        this.pageSize = pageSize;
    }

    public OrderQuery(String orderNo, long buyerId, String status,
                      int page, int pageSize) {
        this(orderNo, Long.valueOf(buyerId), status, page, pageSize);
    }

    public String getOrderNo() { return orderNo; }
    public Long getBuyerId() { return buyerId; }
    public Long getUserId() { return buyerId; }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
