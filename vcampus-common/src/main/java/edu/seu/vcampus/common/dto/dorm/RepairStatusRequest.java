package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 报修工单状态流转请求。 */
public final class RepairStatusRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long orderId;
    private final String status;
    private final Integer evaluationScore;
    private final String evaluationNote;

    public RepairStatusRequest(long orderId, String status, Integer evaluationScore,
                               String evaluationNote) {
        this.orderId = orderId;
        this.status = status;
        this.evaluationScore = evaluationScore;
        this.evaluationNote = evaluationNote;
    }

    public RepairStatusRequest(long orderId, String status) {
        this(orderId, status, null, null);
    }

    public long getOrderId() { return orderId; }
    public long getRepairOrderId() { return orderId; }
    public String getStatus() { return status; }
    public Integer getEvaluationScore() { return evaluationScore; }
    public String getEvaluationNote() { return evaluationNote; }
}
