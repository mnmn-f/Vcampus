package edu.seu.vcampus.common.dto.store;

import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 商店促销规则；类型为 THRESHOLD、PERCENT 或 FIXED。 */
public final class PromotionDto extends PromotionData {
    private static final long serialVersionUID = 1L;
    private final long id;

    public PromotionDto(long id, String code, String name, String type, BigDecimal threshold,
                        BigDecimal value, String productScope, Long productId, String categoryCode,
                        LocalDateTime startsAt, LocalDateTime endsAt, boolean stackable,
                        boolean active) {
        super(code, name, type, threshold, value, productScope, productId, categoryCode,
                startsAt, endsAt, stackable, active);
        this.id = id;
    }
    public long getId() { return id; }
}
