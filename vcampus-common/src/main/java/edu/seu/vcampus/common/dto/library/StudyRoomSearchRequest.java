package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 自习室检索条件和可选时段筛选。 */
public final class StudyRoomSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String keyword;
    private final String status;
    private final Integer minCapacity;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final int page;
    private final int pageSize;

    public StudyRoomSearchRequest(String keyword, String status, Integer minCapacity,
                                  LocalDateTime startAt, LocalDateTime endAt,
                                  int page, int pageSize) {
        this.keyword = keyword;
        this.status = status;
        this.minCapacity = minCapacity;
        this.startAt = startAt;
        this.endAt = endAt;
        this.page = page;
        this.pageSize = pageSize;
    }

    public StudyRoomSearchRequest() {
        this(null, null, null, null, null, 1, 20);
    }

    public String getKeyword() { return keyword; }
    public String getStatus() { return status; }
    public Integer getMinCapacity() { return minCapacity; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
