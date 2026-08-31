package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 宿舍公告视图。 */
public final class DormAnnouncementDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String title;
    private final String content;
    private final String visibleScope;
    private final Long targetRoleId;
    private final String status;
    private final LocalDateTime publishAt;
    private final LocalDateTime expireAt;
    private final long publisherId;

    public DormAnnouncementDto(long id, String title, String content,
                               String visibleScope, Long targetRoleId, String status,
                               LocalDateTime publishAt, LocalDateTime expireAt,
                               long publisherId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.visibleScope = visibleScope;
        this.targetRoleId = targetRoleId;
        this.status = status;
        this.publishAt = publishAt;
        this.expireAt = expireAt;
        this.publisherId = publisherId;
    }

    public long getId() { return id; }
    public long getAnnouncementId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getVisibleScope() { return visibleScope; }
    public Long getTargetRoleId() { return targetRoleId; }
    public String getStatus() { return status; }
    public LocalDateTime getPublishAt() { return publishAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public long getPublisherId() { return publisherId; }
}
