package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 系统管理员重置用户密码请求。 */
public final class PasswordResetRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long userId;
    private final String newPassword;

    public PasswordResetRequest(long userId, String newPassword) {
        this.userId = userId;
        this.newPassword = newPassword;
    }

    public long getUserId() { return userId; }
    public long getId() { return userId; }
    public String getNewPassword() { return newPassword; }
}
