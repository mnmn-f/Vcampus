package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 本人水电分摊缴费请求；幂等键用于网络重试防重复扣款。 */
public final class UtilityPaymentRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long allocationId;
    private final String idempotencyKey;

    public UtilityPaymentRequest(long allocationId, String idempotencyKey) {
        this.allocationId = allocationId;
        this.idempotencyKey = idempotencyKey;
    }

    public long getAllocationId() { return allocationId; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
