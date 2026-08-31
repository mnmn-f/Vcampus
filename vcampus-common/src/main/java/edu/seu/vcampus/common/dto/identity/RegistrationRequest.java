package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 公开注册请求；注册服务始终分配 STUDENT 角色。 */
public final class RegistrationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String account;
    private final String password;
    private final String displayName;
    private final String email;
    private final String phone;

    public RegistrationRequest(String account, String password, String displayName,
                               String email, String phone) {
        this.account = account;
        this.password = password;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
    }

    public RegistrationRequest(String account, String password, String displayName) {
        this(account, password, displayName, null, null);
    }

    public String getAccount() { return account; }
    public String getUsername() { return account; }
    public String getPassword() { return password; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
