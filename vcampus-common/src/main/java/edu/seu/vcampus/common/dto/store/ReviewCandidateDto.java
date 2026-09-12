package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 本人已完成且尚未评价的订单商品。 */
public final class ReviewCandidateDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long orderId, productId;
    private final String orderNo, productName;
    private final int quantity;
    public ReviewCandidateDto(long orderId, long productId, String orderNo, String productName, int quantity) {
        this.orderId = orderId; this.productId = productId; this.orderNo = orderNo; this.productName = productName; this.quantity = quantity;
    }
    public long getOrderId() { return orderId; }
    public long getProductId() { return productId; }
    public String getOrderNo() { return orderNo; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
}
