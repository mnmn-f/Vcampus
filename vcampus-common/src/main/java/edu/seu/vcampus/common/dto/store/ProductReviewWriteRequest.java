package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 已完成订单商品评价请求。 */
public final class ProductReviewWriteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long orderId;
    private final long productId;
    private final int score;
    private final String content;
    public ProductReviewWriteRequest(long orderId, long productId, int score, String content) {
        this.orderId = orderId; this.productId = productId; this.score = score; this.content = content;
    }
    public long getOrderId() { return orderId; }
    public long getProductId() { return productId; }
    public int getScore() { return score; }
    public String getContent() { return content; }
}
