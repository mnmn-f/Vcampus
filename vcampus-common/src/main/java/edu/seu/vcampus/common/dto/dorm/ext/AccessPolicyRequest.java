package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalTime;

/** 修改门禁策略。 */
public final class AccessPolicyRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalTime curfewTime;
    private final LocalTime dawnTime;

    public AccessPolicyRequest(LocalTime curfewTime, LocalTime dawnTime) {
        this.curfewTime = curfewTime;
        this.dawnTime = dawnTime;
    }

    public LocalTime getCurfewTime() { return curfewTime; }
    public LocalTime getDawnTime() { return dawnTime; }
}
