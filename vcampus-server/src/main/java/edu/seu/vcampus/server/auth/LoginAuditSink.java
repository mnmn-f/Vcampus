package edu.seu.vcampus.server.auth;

import edu.seu.vcampus.common.security.Role;

/** AuthService 的可选登录审计出口；实现不得接收或保存明文密码。 */
public interface LoginAuditSink {
    void record(Long userId, String account, Role selectedRole, String resultCode,
                String clientIp);

    LoginAuditSink NOOP = new LoginAuditSink() {
        @Override public void record(Long userId, String account, Role selectedRole,
                                     String resultCode, String clientIp) { }
    };
}
