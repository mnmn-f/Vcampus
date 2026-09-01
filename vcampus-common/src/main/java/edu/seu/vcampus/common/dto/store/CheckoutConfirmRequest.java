package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 结算确认请求；不含任何客户端计算出的最终金额。 */
public final class CheckoutConfirmRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String couponCode;
    private final String paymentMode;
    private final String idempotencyKey;

    public CheckoutConfirmRequest(String couponCode, String paymentMode,
                                  String idempotencyKey) {
        this.couponCode = couponCode;
        this.paymentMode = paymentMode == null ? "SELF" : paymentMode;
        this.idempotencyKey = idempotencyKey;
    }
    public CheckoutConfirmRequest(String couponCode) { this(couponCode, "SELF", null); }
    public String getCouponCode() { return couponCode; }
    public String getPaymentMode() { return paymentMode; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
