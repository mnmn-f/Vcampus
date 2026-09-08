package edu.seu.vcampus.common.dto.identity;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;

/** 认证层写入登录审计的适配请求；不携带密码。 */
public final class LoginAuditWriteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long userId;
    private final String usernameSnapshot;
    private final Role role;
    private final String resultCode;
    private final String clientIp;

    public LoginAuditWriteRequest(Long userId, String usernameSnapshot, Role role,
                                  String resultCode, String clientIp) {
        this.userId = userId;
        this.usernameSnapshot = usernameSnapshot;
        this.role = role;
        this.resultCode = resultCode;
        this.clientIp = clientIp;
    }

    public LoginAuditWriteRequest(long userId, String usernameSnapshot, Role role,
                                  String resultCode, String clientIp) {
        this(Long.valueOf(userId), usernameSnapshot, role, resultCode, clientIp);
    }

    public Long getUserId() { return userId; }
    public String getUsernameSnapshot() { return usernameSnapshot; }
    public Role getRole() { return role; }
    public String getResultCode() { return resultCode; }
    public String getClientIp() { return clientIp; }
}
