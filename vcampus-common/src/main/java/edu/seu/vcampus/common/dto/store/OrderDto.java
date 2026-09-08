package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 订单及其价格快照明细。 */
public final class OrderDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String orderNo;
    private final long buyerId;
    private final BigDecimal totalAmount;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final String promotionCode;
    private final String couponCode;
    private final String paymentMode;
    private final String status;
    private final String shippingStatus;
    private final String trackingNo;
    private final String shippingRemark;
    private final LocalDateTime createdAt;
    private final LocalDateTime paidAt;
    private final LocalDateTime cancelledAt;
    private final LocalDateTime completedAt;
    private final List<OrderItemDto> items;

    public OrderDto(long id, String orderNo, long buyerId, BigDecimal totalAmount,
                    String status, LocalDateTime createdAt, LocalDateTime paidAt,
                    LocalDateTime cancelledAt, LocalDateTime completedAt,
                    List<OrderItemDto> items) {
        this(id, orderNo, buyerId, totalAmount, totalAmount, BigDecimal.ZERO, null, null,
                "SELF", status, null, null, null, createdAt, paidAt, cancelledAt, completedAt, items);
    }

    public OrderDto(long id, String orderNo, long buyerId, BigDecimal totalAmount,
                    String status, List<OrderItemDto> items) {
        this(id, orderNo, buyerId, totalAmount, totalAmount, BigDecimal.ZERO, null, null,
                "SELF", status, null, null, null, null, null, null, null, items);
    }

    public OrderDto(long id, String orderNo, long buyerId, BigDecimal totalAmount,
                    BigDecimal originalAmount, BigDecimal discountAmount, String promotionCode,
                    String couponCode, String paymentMode, String status,
                    LocalDateTime createdAt, LocalDateTime paidAt, LocalDateTime cancelledAt,
                    LocalDateTime completedAt, List<OrderItemDto> items) {
        this(id, orderNo, buyerId, totalAmount, originalAmount, discountAmount, promotionCode,
                couponCode, paymentMode, status, null, null, null, createdAt, paidAt,
                cancelledAt, completedAt, items);
    }

    public OrderDto(long id, String orderNo, long buyerId, BigDecimal totalAmount,
                    BigDecimal originalAmount, BigDecimal discountAmount, String promotionCode,
                    String couponCode, String paymentMode, String status, String shippingStatus,
                    String trackingNo, String shippingRemark, LocalDateTime createdAt,
                    LocalDateTime paidAt, LocalDateTime cancelledAt, LocalDateTime completedAt,
                    List<OrderItemDto> items) {
        this.id = id; this.orderNo = orderNo; this.buyerId = buyerId; this.totalAmount = totalAmount;
        this.originalAmount = originalAmount == null ? totalAmount : originalAmount;
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        this.promotionCode = promotionCode; this.couponCode = couponCode;
        this.paymentMode = paymentMode == null ? "SELF" : paymentMode; this.status = status;
        this.shippingStatus = shippingStatus; this.trackingNo = trackingNo;
        this.shippingRemark = shippingRemark;
        this.createdAt = createdAt; this.paidAt = paidAt; this.cancelledAt = cancelledAt;
        this.completedAt = completedAt;
        this.items = Collections.unmodifiableList(new ArrayList<OrderItemDto>(
                items == null ? Collections.<OrderItemDto>emptyList() : items));
    }

    public long getId() { return id; }
    public long getOrderId() { return id; }
    public String getOrderNo() { return orderNo; }
    public long getBuyerId() { return buyerId; }
    public long getBuyerUserId() { return buyerId; }
    public long getUserId() { return buyerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getTotal() { return totalAmount; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getPromotionCode() { return promotionCode; }
    public String getCouponCode() { return couponCode; }
    public String getPaymentMode() { return paymentMode; }
    public String getStatus() { return status; }
    public String getShippingStatus() { return shippingStatus; }
    public String getTrackingNo() { return trackingNo; }
    public String getShippingRemark() { return shippingRemark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public List<OrderItemDto> getItems() { return items; }
}
