package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;
import java.util.Locale;

/** 公告创建、修改和撤回请求；publisherId 不属于客户端输入。 */
public final class CampusAnnouncementSaveRequest extends CampusAnnouncementData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long id;
    public CampusAnnouncementSaveRequest(Long id, String moduleCode, String title,
                                         String content, String visibleScope,
                                         Long targetRoleId, String status,
                                         LocalDateTime publishAt, LocalDateTime expireAt) {
        this(id, moduleCode, title, content, visibleScope, targetRoleId, null, status,
                publishAt, expireAt);
    }

    public CampusAnnouncementSaveRequest(Long id, String moduleCode, String title,
                                         String content, String visibleScope,
                                         Long targetRoleId, String targetRoleCode,
                                         String status, LocalDateTime publishAt,
                                         LocalDateTime expireAt) {
        super(code(moduleCode), title, content, code(visibleScope), targetRoleId,
                code(targetRoleCode), code(status), publishAt, expireAt);
        this.id = id;
    }

    public static CampusAnnouncementSaveRequest create(String module, String title,
            String content, String scope, Long roleId, String status,
            LocalDateTime publishAt, LocalDateTime expireAt) {
        return new CampusAnnouncementSaveRequest(null, module, title, content, scope,
                roleId, status, publishAt, expireAt);
    }

    public static CampusAnnouncementSaveRequest update(long id, String module, String title,
            String content, String scope, Long roleId, String status,
            LocalDateTime publishAt, LocalDateTime expireAt) {
        return new CampusAnnouncementSaveRequest(Long.valueOf(id), module, title, content,
                scope, roleId, status, publishAt, expireAt);
    }

    public Long getId() { return id; }
    public Long getAnnouncementId() { return id; }
    public boolean isUpdate() { return id != null && id.longValue() > 0L; }

    private static String code(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
