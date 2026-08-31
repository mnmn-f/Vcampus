package edu.seu.vcampus.common.dto.identity;

import edu.seu.vcampus.common.security.Role;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 业务审计记录。detailJson 只作为展示文本，不在客户端执行。 */
public final class BusinessAuditDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final Long actorUserId;
    private final Role actorRole;
    private final String action;
    private final String resourceType;
    private final Long resourceId;
    private final String outcome;
    private final String detailJson;
    private final LocalDateTime occurredAt;

    public BusinessAuditDto(long id, Long actorUserId, Role actorRole, String action,
                            String resourceType, Long resourceId, String outcome,
                            String detailJson, LocalDateTime occurredAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.detailJson = detailJson;
        this.occurredAt = occurredAt;
    }

    public long getId() { return id; }
    public Long getActorUserId() { return actorUserId; }
    public Role getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public String getOutcome() { return outcome; }
    public String getDetailJson() { return detailJson; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
