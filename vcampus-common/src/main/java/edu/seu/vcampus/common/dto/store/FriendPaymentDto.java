package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 脱敏代付请求；不包含付款人余额。 */
public final class FriendPaymentDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long orderId;
    private final String orderNo;
    private final long buyerId;
    private final String buyerName;
    private final long payerId;
    private final String payerName;
    private final BigDecimal amount;
    private final String status;
    private final String message;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    public FriendPaymentDto(long id, long orderId, String orderNo, long buyerId,
            String buyerName, long payerId, String payerName, BigDecimal amount,
            String status, String message, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.id = id; this.orderId = orderId; this.orderNo = orderNo; this.buyerId = buyerId;
        this.buyerName = buyerName; this.payerId = payerId; this.payerName = payerName;
        this.amount = amount; this.status = status; this.message = message;
        this.expiresAt = expiresAt; this.createdAt = createdAt;
    }
    public long getId() { return id; }
    public long getRequestId() { return id; }
    public long getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public long getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public long getPayerId() { return payerId; }
    public String getPayerName() { return payerName; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
