package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/** 修改未归预警阈值。 */
public final class WarningConfigRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int warnDays;
    private final int notifyDays;
    private final boolean exemptOnLeave;

    public WarningConfigRequest(int warnDays, int notifyDays, boolean exemptOnLeave) {
        this.warnDays = warnDays;
        this.notifyDays = notifyDays;
        this.exemptOnLeave = exemptOnLeave;
    }

    public int getWarnDays() { return warnDays; }
    public int getNotifyDays() { return notifyDays; }
    public boolean isExemptOnLeave() { return exemptOnLeave; }
}
