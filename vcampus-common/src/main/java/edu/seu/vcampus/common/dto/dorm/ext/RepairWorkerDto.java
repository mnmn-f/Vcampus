package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/**
 * 可派单的维修员。
 *
 * <p>只带派单时要看的三件事：是谁、手上压着几单、账号还启用着没有。工种、班次这些
 * 属性系统里没有存，也没有任何功能会读，不在这里凭空造字段。</p>
 */
public final class RepairWorkerDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final String displayName;
    private final int activeOrders;

    public RepairWorkerDto(long userId, String displayName, int activeOrders) {
        this.userId = userId;
        this.displayName = displayName;
        this.activeOrders = activeOrders;
    }

    public long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public int getActiveOrders() { return activeOrders; }

    /** 派单列表里显示成「张师傅 · 手头 2 单」。 */
    public String summary() {
        return (displayName == null ? "维修员 " + userId : displayName) + " · 手头 " + activeOrders + " 单";
    }
}
