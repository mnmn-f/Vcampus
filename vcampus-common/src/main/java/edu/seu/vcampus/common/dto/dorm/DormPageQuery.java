package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 楼栋、房间、床位及治理记录共用的安全分页筛选。 */
public final class DormPageQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int page;
    private final int pageSize;
    private final String keyword;
    private final String status;
    private final Long buildingId;
    private final Long roomId;

    public DormPageQuery(int page, int pageSize, String keyword, String status,
                         Long buildingId, Long roomId) {
        this.page = page;
        this.pageSize = pageSize;
        this.keyword = keyword;
        this.status = status;
        this.buildingId = buildingId;
        this.roomId = roomId;
    }

    public DormPageQuery() {
        this(1, 20, null, null, null, null);
    }

    public static DormPageQuery all() { return new DormPageQuery(); }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public String getKeyword() { return keyword; }
    public String getStatus() { return status; }
    public Long getBuildingId() { return buildingId; }
    public Long getRoomId() { return roomId; }
}
