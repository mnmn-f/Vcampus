package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 结算预览请求；金额完全由服务端计算。 */
public final class CheckoutPreviewRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String couponCode;
    public CheckoutPreviewRequest(String couponCode) {
        this.couponCode = couponCode == null || couponCode.trim().isEmpty()
                ? null : couponCode.trim();
    }
    public String getCouponCode() { return couponCode; }
}
