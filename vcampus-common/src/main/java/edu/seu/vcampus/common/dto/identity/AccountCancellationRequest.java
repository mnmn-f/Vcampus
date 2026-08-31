package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 本人账号注销申请；用户身份由服务端会话提供。 */
public final class AccountCancellationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String reason;

    public AccountCancellationRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
