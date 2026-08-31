package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 商店管理员推进或取消订单状态的请求。 */
public final class OrderStatusUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long orderId;
    private final String status;
    private final String remark;

    public OrderStatusUpdateRequest(long orderId, String status, String remark) {
        this.orderId = orderId;
        this.status = status;
        this.remark = remark;
    }

    public OrderStatusUpdateRequest(long orderId, String status) {
        this(orderId, status, null);
    }

    public long getOrderId() { return orderId; }
    public long getId() { return orderId; }
    public String getStatus() { return status; }
    public String getRemark() { return remark; }
}
