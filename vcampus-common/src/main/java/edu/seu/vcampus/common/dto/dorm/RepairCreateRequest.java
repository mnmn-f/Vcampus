package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 报修工单创建请求；reporter 由会话身份决定。 */
public final class RepairCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long roomId;
    private final String category;
    private final String description;
    private final String priority;

    public RepairCreateRequest(long roomId, String category, String description,
                               String priority) {
        this.roomId = roomId;
        this.category = category;
        this.description = description;
        this.priority = priority;
    }

    public long getRoomId() { return roomId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
}
