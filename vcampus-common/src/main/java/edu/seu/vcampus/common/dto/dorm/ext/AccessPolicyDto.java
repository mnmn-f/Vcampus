package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/** 门禁策略：晚于门禁时间、或早于清晨时间归宿，都记晚归。 */
public final class AccessPolicyDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalTime curfewTime;
    private final LocalTime dawnTime;
    private final LocalDateTime updatedAt;

    public AccessPolicyDto(LocalTime curfewTime, LocalTime dawnTime, LocalDateTime updatedAt) {
        this.curfewTime = curfewTime;
        this.dawnTime = dawnTime;
        this.updatedAt = updatedAt;
    }

    public LocalTime getCurfewTime() { return curfewTime; }
    public LocalTime getDawnTime() { return dawnTime; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
