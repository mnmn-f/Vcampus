package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 线上资源检索条件。普通用户只能检索 ACTIVE 资源。 */
public final class OnlineResourceSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String keyword;
    private final String resourceType;
    private final String status;
    private final int page;
    private final int pageSize;

    public OnlineResourceSearchRequest(String keyword, String resourceType,
                                       String status, int page, int pageSize) {
        this.keyword = keyword;
        this.resourceType = resourceType;
        this.status = status;
        this.page = page;
        this.pageSize = pageSize;
    }

    public OnlineResourceSearchRequest() {
        this(null, null, null, 1, 20);
    }

    public String getKeyword() { return keyword; }
    public String getResourceType() { return resourceType; }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
