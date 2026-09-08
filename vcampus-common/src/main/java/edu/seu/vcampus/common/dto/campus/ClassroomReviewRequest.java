package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import java.util.Locale;

/** 教务老师审批或撤销教室申请；reviewerId 从 SessionContext 获取。 */
public final class ClassroomReviewRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long reservationId;
    private final String status;
    private final String remark;

    public ClassroomReviewRequest(long reservationId, String status, String remark) {
        this.reservationId = reservationId;
        this.status = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
        this.remark = remark;
    }

    public ClassroomReviewRequest(long reservationId, boolean approve, String remark) {
        this(reservationId, approve ? "APPROVED" : "REJECTED", remark);
    }

    public long getReservationId() { return reservationId; }
    public long getId() { return reservationId; }
    public String getStatus() { return status; }
    public String getRemark() { return remark; }
}
