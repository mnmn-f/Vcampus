package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 线上资源公开信息。 */
public final class OnlineResourceView implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String title;
    private final String resourceType;
    private final String url;
    private final String description;
    private final long publisherId;
    private final String status;
    private final LocalDateTime publishedAt;

    public OnlineResourceView(long id, String title, String resourceType,
                              String url, String description, long publisherId,
                              String status, LocalDateTime publishedAt) {
        this.id = id;
        this.title = title;
        this.resourceType = resourceType;
        this.url = url;
        this.description = description;
        this.publisherId = publisherId;
        this.status = status;
        this.publishedAt = publishedAt;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getResourceType() { return resourceType; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
    public long getPublisherId() { return publisherId; }
    public String getStatus() { return status; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}
