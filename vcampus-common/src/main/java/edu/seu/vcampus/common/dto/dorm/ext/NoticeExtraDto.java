package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/**
 * 宿舍公告的完整视图：main 的 {@code announcements} 正文 + 旁挂的类型/范围/置顶。
 *
 * <p>设计文档的 Notice 实体比 main 的 announcements 多三组属性，但 announcements
 * 是七个模块共用的表，加列会波及所有模块。这里改成旁挂一张一对一表，没有扩展
 * 属性的旧公告读出来就是默认值（普通类型、全体范围、不置顶），因此 main 已经发布
 * 的公告不需要任何数据迁移。</p>
 */
public final class NoticeExtraDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 普通通知。 */
    public static final String TYPE_GENERAL = "GENERAL";
    /** 维修/停水停电。 */
    public static final String TYPE_MAINTENANCE = "MAINTENANCE";
    /** 卫生检查相关。 */
    public static final String TYPE_HYGIENE = "HYGIENE";
    /** 安全提醒。 */
    public static final String TYPE_SAFETY = "SAFETY";
    /** 紧急通知。 */
    public static final String TYPE_URGENT = "URGENT";

    /** 面向全体住宿学生。 */
    public static final String SCOPE_ALL = "ALL";
    /** 只面向某栋楼。 */
    public static final String SCOPE_BUILDING = "BUILDING";
    /** 只面向某个房间。 */
    public static final String SCOPE_ROOM = "ROOM";

    private final long announcementId;
    private final String title;
    private final String content;
    private final String status;
    private final LocalDateTime publishAt;
    private final LocalDateTime expireAt;
    private final long publisherId;
    private final String noticeType;
    private final String scopeType;
    private final Long scopeBuildingId;
    private final Long scopeRoomId;
    private final String buildingCode;
    private final String roomNo;
    private final boolean pinned;
    private final LocalDateTime pinnedAt;

    public NoticeExtraDto(long announcementId, String title, String content, String status,
                          LocalDateTime publishAt, LocalDateTime expireAt, long publisherId,
                          String noticeType, String scopeType, Long scopeBuildingId,
                          Long scopeRoomId, String buildingCode, String roomNo,
                          boolean pinned, LocalDateTime pinnedAt) {
        this.announcementId = announcementId;
        this.title = title;
        this.content = content;
        this.status = status;
        this.publishAt = publishAt;
        this.expireAt = expireAt;
        this.publisherId = publisherId;
        this.noticeType = noticeType == null ? TYPE_GENERAL : noticeType;
        this.scopeType = scopeType == null ? SCOPE_ALL : scopeType;
        this.scopeBuildingId = scopeBuildingId;
        this.scopeRoomId = scopeRoomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.pinned = pinned;
        this.pinnedAt = pinnedAt;
    }

    public long getAnnouncementId() { return announcementId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public LocalDateTime getPublishAt() { return publishAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public long getPublisherId() { return publisherId; }
    public String getNoticeType() { return noticeType; }
    public String getScopeType() { return scopeType; }
    public Long getScopeBuildingId() { return scopeBuildingId; }
    public Long getScopeRoomId() { return scopeRoomId; }
    /** 范围为楼栋或房间时的楼栋编码；范围为全体时为 null。 */
    public String getBuildingCode() { return buildingCode; }
    /** 范围为房间时的房间号；否则为 null。 */
    public String getRoomNo() { return roomNo; }
    public boolean isPinned() { return pinned; }
    public LocalDateTime getPinnedAt() { return pinnedAt; }

    public static String typeName(String value) {
        if (TYPE_MAINTENANCE.equals(value)) return "维修通知";
        if (TYPE_HYGIENE.equals(value)) return "卫生通知";
        if (TYPE_SAFETY.equals(value)) return "安全提醒";
        if (TYPE_URGENT.equals(value)) return "紧急通知";
        return "普通通知";
    }

    public static String scopeName(String value) {
        if (SCOPE_BUILDING.equals(value)) return "本楼栋";
        if (SCOPE_ROOM.equals(value)) return "本房间";
        return "全体";
    }

    /** 「本楼栋 D1」这样的一行展示文本，供表格与列表直接使用。 */
    public String scopeText() {
        if (SCOPE_ROOM.equals(scopeType)) {
            return scopeName(scopeType) + " "
                    + (buildingCode == null ? "" : buildingCode)
                    + (roomNo == null ? "" : roomNo);
        }
        if (SCOPE_BUILDING.equals(scopeType)) {
            return scopeName(scopeType) + " " + (buildingCode == null ? "" : buildingCode);
        }
        return scopeName(scopeType);
    }
}
