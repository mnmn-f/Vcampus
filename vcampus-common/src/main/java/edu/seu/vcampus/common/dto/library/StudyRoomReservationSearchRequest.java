package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 自习室预约记录查询条件。学生查询时只能看到自己的记录。 */
public final class StudyRoomReservationSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String status;
    private final int page;
    private final int pageSize;

    public StudyRoomReservationSearchRequest(String status, int page, int pageSize) {
        this.status = status;
        this.page = page;
        this.pageSize = pageSize;
    }

    public StudyRoomReservationSearchRequest() { this(null, 1, 20); }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
