package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 领取优惠券请求。 */
public final class CouponClaimRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String code;
    public CouponClaimRequest(String code) { this.code = code; }
    public String getCode() { return code; }
}
