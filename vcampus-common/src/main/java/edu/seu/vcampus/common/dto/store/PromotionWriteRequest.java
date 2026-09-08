package edu.seu.vcampus.common.dto.store;

import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 促销规则维护请求；适用范围可为 ALL、PRODUCT 或 CATEGORY。 */
public final class PromotionWriteRequest extends PromotionData {
    private static final long serialVersionUID = 1L;
    private final long id;

    public PromotionWriteRequest(long id, String code, String name, String type,
            BigDecimal threshold, BigDecimal value, String productScope, Long productId,
            String categoryCode, LocalDateTime startsAt, LocalDateTime endsAt,
            boolean stackable, boolean active) {
        super(code, name, type, threshold, value, productScope, productId, categoryCode,
                startsAt, endsAt, stackable, active);
        this.id = id;
    }
    public long getId() { return id; }
}
