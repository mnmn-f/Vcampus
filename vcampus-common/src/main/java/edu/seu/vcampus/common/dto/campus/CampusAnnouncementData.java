package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 公告视图和写请求共享的业务字段，避免协议对象复制字段访问器。 */
public abstract class CampusAnnouncementData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String moduleCode;
    private final String title;
    private final String content;
    private final String visibleScope;
    private final Long targetRoleId;
    private final String targetRoleCode;
    private final String status;
    private final LocalDateTime publishAt;
    private final LocalDateTime expireAt;

    protected CampusAnnouncementData(String moduleCode, String title, String content,
            String visibleScope, Long targetRoleId, String targetRoleCode, String status,
            LocalDateTime publishAt, LocalDateTime expireAt) {
        this.moduleCode = moduleCode;
        this.title = title;
        this.content = content;
        this.visibleScope = visibleScope;
        this.targetRoleId = targetRoleId;
        this.targetRoleCode = targetRoleCode;
        this.status = status;
        this.publishAt = publishAt;
        this.expireAt = expireAt;
    }

    public String getModuleCode() { return moduleCode; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getVisibleScope() { return visibleScope; }
    public Long getTargetRoleId() { return targetRoleId; }
    public String getTargetRoleCode() { return targetRoleCode; }
    public String getStatus() { return status; }
    public LocalDateTime getPublishAt() { return publishAt; }
    public LocalDateTime getPublishTime() { return publishAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public LocalDateTime getExpireTime() { return expireAt; }
}
