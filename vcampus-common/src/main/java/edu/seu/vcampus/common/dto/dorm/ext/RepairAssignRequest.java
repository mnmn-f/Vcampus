package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/** 宿管把一张报修工单派给某个维修员。 */
public final class RepairAssignRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long orderId;
    private final long workerUserId;

    public RepairAssignRequest(long orderId, long workerUserId) {
        this.orderId = orderId;
        this.workerUserId = workerUserId;
    }

    public long getOrderId() { return orderId; }
    public long getWorkerUserId() { return workerUserId; }
}
