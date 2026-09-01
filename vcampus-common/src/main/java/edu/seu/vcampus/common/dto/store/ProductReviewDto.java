package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 商品评价及其匿名化展示信息。 */
public final class ProductReviewDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long productId;
    private final long orderId;
    private final String productName;
    private final int score;
    private final String content;
    private final String reviewerName;
    private final LocalDateTime createdAt;

    public ProductReviewDto(long id, long productId, long orderId, String productName,
                            int score, String content, String reviewerName,
                            LocalDateTime createdAt) {
        this.id = id; this.productId = productId; this.orderId = orderId;
        this.productName = productName; this.score = score; this.content = content;
        this.reviewerName = reviewerName; this.createdAt = createdAt;
    }
    public long getId() { return id; }
    public long getProductId() { return productId; }
    public long getOrderId() { return orderId; }
    public String getProductName() { return productName; }
    public int getScore() { return score; }
    public String getContent() { return content; }
    public String getReviewerName() { return reviewerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
