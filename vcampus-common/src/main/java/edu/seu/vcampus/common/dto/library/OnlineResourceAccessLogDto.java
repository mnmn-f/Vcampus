package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 线上资源访问摘要；不包含客户端地址、令牌或其他敏感字段。 */
public final class OnlineResourceAccessLogDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long resourceId;
    private final long userId;
    private final String resourceTitle;
    private final String account;
    private final String displayName;
    private final LocalDateTime accessedAt;

    public OnlineResourceAccessLogDto(long id, long resourceId, long userId,
                                      String resourceTitle, String account,
                                      String displayName, LocalDateTime accessedAt) {
        this.id = id;
        this.resourceId = resourceId;
        this.userId = userId;
        this.resourceTitle = resourceTitle;
        this.account = account;
        this.displayName = displayName;
        this.accessedAt = accessedAt;
    }

    public long getId() { return id; }
    public long getResourceId() { return resourceId; }
    public long getUserId() { return userId; }
    public String getResourceTitle() { return resourceTitle; }
    public String getAccount() { return account; }
    public String getDisplayName() { return displayName; }
    public LocalDateTime getAccessedAt() { return accessedAt; }
}
