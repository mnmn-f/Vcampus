package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 余额支付请求；幂等键由客户端为一次业务意图稳定生成。 */
public final class PaymentRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long orderId;
    private final String idempotencyKey;
    private final String remark;

    public PaymentRequest(long orderId, String idempotencyKey, String remark) {
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.remark = remark;
    }

    public PaymentRequest(long orderId, String idempotencyKey) {
        this(orderId, idempotencyKey, null);
    }

    public long getOrderId() { return orderId; }
    public long getId() { return orderId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRemark() { return remark; }
}
