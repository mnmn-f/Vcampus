package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import java.util.Locale;

/** 公告、比赛、项目和教室列表共用的分页筛选。 */
public final class CampusPageQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final int page;
    private final int pageSize;
    private final String keyword;
    private final String status;
    private final String moduleCode;
    private final String roleCode;

    public CampusPageQuery() { this(1, DEFAULT_PAGE_SIZE, null, null, null, null); }

    public CampusPageQuery(int page, int pageSize, String keyword, String status) {
        this(page, pageSize, keyword, status, null, null);
    }

    public CampusPageQuery(int page, int pageSize, String keyword, String status,
                           String moduleCode, String roleCode) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page is out of range");
        }
        this.page = page;
        this.pageSize = pageSize;
        this.keyword = text(keyword);
        this.status = code(status);
        this.moduleCode = code(moduleCode);
        this.roleCode = code(roleCode);
    }

    public static CampusPageQuery all() { return new CampusPageQuery(); }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public String getKeyword() { return keyword; }
    public String getStatus() { return status; }
    public String getModuleCode() { return moduleCode; }
    public String getRoleCode() { return roleCode; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String code(String value) {
        String text = text(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }
}
