package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 学生对本人已完成报修工单的一次评价。 */
public final class RepairEvaluationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long orderId;
    private final int score;
    private final String note;

    public RepairEvaluationRequest(long orderId, int score, String note) {
        this.orderId = orderId; this.score = score; this.note = note;
    }

    public long getOrderId() { return orderId; }
    public long getRepairOrderId() { return orderId; }
    public int getScore() { return score; }
    public int getEvaluationScore() { return score; }
    public String getNote() { return note; }
    public String getEvaluationNote() { return note; }
}
