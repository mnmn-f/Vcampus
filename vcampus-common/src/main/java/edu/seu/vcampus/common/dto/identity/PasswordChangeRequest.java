package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 本人修改密码请求。 */
public final class PasswordChangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String currentPassword;
    private final String newPassword;

    public PasswordChangeRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() { return currentPassword; }
    public String getOldPassword() { return currentPassword; }
    public String getNewPassword() { return newPassword; }
}
