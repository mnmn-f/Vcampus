package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 未归预警一次性处理动作。 */
public final class LateReturnHandleRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long alertId;
    private final String status;
    private final String note;

    public LateReturnHandleRequest(long alertId, String status, String note) {
        this.alertId = alertId;
        this.status = status;
        this.note = note;
    }

    public long getAlertId() { return alertId; }
    public String getStatus() { return status; }
    public String getNote() { return note; }
}
