package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 未归预警阈值配置。 */
public final class WarningConfigDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int warnDays;
    private final int notifyDays;
    private final boolean exemptOnLeave;
    private final LocalDateTime updatedAt;

    public WarningConfigDto(int warnDays, int notifyDays, boolean exemptOnLeave,
                            LocalDateTime updatedAt) {
        this.warnDays = warnDays;
        this.notifyDays = notifyDays;
        this.exemptOnLeave = exemptOnLeave;
        this.updatedAt = updatedAt;
    }

    /** 连续未归达到这个天数开始生成预警。 */
    public int getWarnDays() { return warnDays; }
    /** 连续未归达到这个天数升为严重，应当通知辅导员。 */
    public int getNotifyDays() { return notifyDays; }
    /** 已批准的请假是否豁免预警。 */
    public boolean isExemptOnLeave() { return exemptOnLeave; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
