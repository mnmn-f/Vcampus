package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 促销规则读写对象共享的数据载荷。 */
public abstract class PromotionData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String code;
    private final String name;
    private final String type;
    private final BigDecimal threshold;
    private final BigDecimal value;
    private final String productScope;
    private final Long productId;
    private final String categoryCode;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final boolean stackable;
    private final boolean active;

    protected PromotionData(String code, String name, String type, BigDecimal threshold,
            BigDecimal value, String productScope, Long productId, String categoryCode,
            LocalDateTime startsAt, LocalDateTime endsAt, boolean stackable, boolean active) {
        this.code = code; this.name = name; this.type = type; this.threshold = threshold;
        this.value = value; this.productScope = productScope; this.productId = productId;
        this.categoryCode = categoryCode; this.startsAt = startsAt; this.endsAt = endsAt;
        this.stackable = stackable; this.active = active;
    }
    public final String getCode() { return code; }
    public final String getName() { return name; }
    public final String getType() { return type; }
    public final BigDecimal getThreshold() { return threshold; }
    public final BigDecimal getValue() { return value; }
    public final String getProductScope() { return productScope; }
    public final Long getProductId() { return productId; }
    public final String getCategoryCode() { return categoryCode; }
    public final LocalDateTime getStartsAt() { return startsAt; }
    public final LocalDateTime getEndsAt() { return endsAt; }
    public final boolean isStackable() { return stackable; }
    public final boolean isActive() { return active; }
}
