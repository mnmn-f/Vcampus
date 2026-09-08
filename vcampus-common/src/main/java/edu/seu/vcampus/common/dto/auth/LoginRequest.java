package edu.seu.vcampus.common.dto.auth;

import java.io.Serializable;

/** 封装客户端提交的登录账号和密码。 */
public final class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String account;
    private final String password;

    public LoginRequest(String account, String password) {
        this.account = account;
        this.password = password;
    }

    public String getAccount() { return account; }
    public String getPassword() { return password; }
}
