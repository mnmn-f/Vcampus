package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 图书管理员新增或维护线上资源。id 为 0 表示新增。 */
public final class OnlineResourceUpsertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String title;
    private final String resourceType;
    private final String url;
    private final String description;
    private final String status;

    public OnlineResourceUpsertRequest(long id, String title, String resourceType,
                                       String url, String description,
                                       String status) {
        this.id = id;
        this.title = title;
        this.resourceType = resourceType;
        this.url = url;
        this.description = description;
        this.status = status;
    }

    public OnlineResourceUpsertRequest(String title, String resourceType,
                                       String url, String description) {
        this(0L, title, resourceType, url, description, "ACTIVE");
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getResourceType() { return resourceType; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
}
