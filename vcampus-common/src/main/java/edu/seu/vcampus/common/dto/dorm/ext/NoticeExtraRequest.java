package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/**
 * 设置某条宿舍公告的类型、可见范围与置顶。
 *
 * <p>公告正文仍由既有的 {@code dorm.announcement.save} 负责，本请求只写旁挂属性，
 * 因此改扩展属性不会碰到标题、正文和发布状态。</p>
 */
public final class NoticeExtraRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long announcementId;
    private final String noticeType;
    private final String scopeType;
    private final Long scopeBuildingId;
    private final Long scopeRoomId;
    private final boolean pinned;

    public NoticeExtraRequest(long announcementId, String noticeType, String scopeType,
                              Long scopeBuildingId, Long scopeRoomId, boolean pinned) {
        this.announcementId = announcementId;
        this.noticeType = noticeType;
        this.scopeType = scopeType;
        this.scopeBuildingId = scopeBuildingId;
        this.scopeRoomId = scopeRoomId;
        this.pinned = pinned;
    }

    public long getAnnouncementId() { return announcementId; }
    public String getNoticeType() { return noticeType; }
    public String getScopeType() { return scopeType; }
    /** 范围为楼栋时必填。 */
    public Long getScopeBuildingId() { return scopeBuildingId; }
    /** 范围为房间时必填。 */
    public Long getScopeRoomId() { return scopeRoomId; }
    public boolean isPinned() { return pinned; }
}
