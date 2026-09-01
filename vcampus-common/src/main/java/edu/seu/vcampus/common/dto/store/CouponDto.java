package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 用户可领取和使用的优惠券。 */
public final class CouponDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String code;
    private final String name;
    private final BigDecimal threshold;
    private final BigDecimal discountAmount;
    private final LocalDateTime expiresAt;
    private final boolean claimed;
    private final boolean used;

    public CouponDto(long id, String code, String name, BigDecimal threshold,
                     BigDecimal discountAmount, LocalDateTime expiresAt,
                     boolean claimed, boolean used) {
        this.id = id; this.code = code; this.name = name; this.threshold = threshold;
        this.discountAmount = discountAmount; this.expiresAt = expiresAt;
        this.claimed = claimed; this.used = used;
    }
    public long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getThreshold() { return threshold; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isClaimed() { return claimed; }
    public boolean isUsed() { return used; }
}
