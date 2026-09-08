package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/**
 * 维修员对某张工单的一次操作（接单、开工、完工）。
 *
 * <p>三个动作共用一个请求对象：它们的入参完全一样（哪张单、附一句说明），差别只在
 * 目标状态，而目标状态由命令名决定，不该再由客户端塞一个字符串进来——那样客户端
 * 就能把工单直接推到任意状态，跳过中间的流转校验。</p>
 */
public final class RepairWorkRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long orderId;
    private final String note;

    public RepairWorkRequest(long orderId, String note) {
        this.orderId = orderId;
        this.note = note;
    }

    public long getOrderId() { return orderId; }
    public String getNote() { return note; }
}
