package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 商店管理员更新订单物流节点。 */
public final class OrderShippingUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long orderId;
    private final String shippingStatus;
    private final String trackingNo;
    private final String remark;

    public OrderShippingUpdateRequest(long orderId, String shippingStatus,
                                      String trackingNo, String remark) {
        this.orderId = orderId; this.shippingStatus = shippingStatus;
        this.trackingNo = trackingNo; this.remark = remark;
    }
    public long getOrderId() { return orderId; }
    public String getShippingStatus() { return shippingStatus; }
    public String getTrackingNo() { return trackingNo; }
    public String getRemark() { return remark; }
}
