package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 跨业务公告视图；发布者身份由服务端会话写入。 */
public final class CampusAnnouncementDto extends CampusAnnouncementData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long publisherId;

    public CampusAnnouncementDto(long id, String moduleCode, String title, String content,
                                 String visibleScope, Long targetRoleId, String status,
                                 LocalDateTime publishAt, LocalDateTime expireAt,
                                 long publisherId) {
        this(id, moduleCode, title, content, visibleScope, targetRoleId, null, status,
                publishAt, expireAt, publisherId);
    }

    public CampusAnnouncementDto(long id, String moduleCode, String title, String content,
                                 String visibleScope, Long targetRoleId, String targetRoleCode,
                                 String status, LocalDateTime publishAt,
                                 LocalDateTime expireAt, long publisherId) {
        super(moduleCode, title, content, visibleScope, targetRoleId, targetRoleCode, status,
                publishAt, expireAt);
        this.id = id;
        this.publisherId = publisherId;
    }

    public long getId() { return id; }
    public long getAnnouncementId() { return id; }
    public long getPublisherId() { return publisherId; }
}
