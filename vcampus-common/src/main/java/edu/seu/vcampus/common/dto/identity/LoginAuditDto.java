package edu.seu.vcampus.common.dto.identity;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 登录审计记录。 */
public final class LoginAuditDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final Long userId;
    private final String usernameSnapshot;
    private final Role role;
    private final String resultCode;
    private final String clientIp;
    private final LocalDateTime occurredAt;

    public LoginAuditDto(long id, Long userId, String usernameSnapshot, Role role,
                         String resultCode, String clientIp, LocalDateTime occurredAt) {
        this.id = id;
        this.userId = userId;
        this.usernameSnapshot = usernameSnapshot;
        this.role = role;
        this.resultCode = resultCode;
        this.clientIp = clientIp;
        this.occurredAt = occurredAt;
    }

    public long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsernameSnapshot() { return usernameSnapshot; }
    public String getAccount() { return usernameSnapshot; }
    public Role getRole() { return role; }
    public String getResultCode() { return resultCode; }
    public String getClientIp() { return clientIp; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
