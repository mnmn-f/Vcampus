package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import java.util.Locale;

/** 公告查询载荷，角色从当前服务端会话获取。 */
public final class CampusAnnouncementQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final CampusPageQuery page;
    private final String moduleCode;

    public CampusAnnouncementQuery(CampusPageQuery page, String moduleCode) {
        this.page = page == null ? CampusPageQuery.all() : page;
        this.moduleCode = moduleCode == null ? null : moduleCode.trim().toUpperCase(Locale.ROOT);
    }

    public CampusAnnouncementQuery() { this(null, null); }
    public CampusPageQuery getPage() { return page; }
    public String getModuleCode() { return moduleCode; }
}
