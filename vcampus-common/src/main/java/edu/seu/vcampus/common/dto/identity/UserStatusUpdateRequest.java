package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 系统管理员启停用户请求。 */
public final class UserStatusUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long userId;
    private final String status;

    public UserStatusUpdateRequest(long userId, String status) {
        this.userId = userId;
        this.status = status;
    }

    public long getUserId() { return userId; }
    public long getId() { return userId; }
    public String getStatus() { return status; }
}
