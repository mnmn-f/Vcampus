package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 宿管水电分摊分页筛选；不包含学生身份，身份由服务端会话和权限决定。 */
public final class UtilityBillQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int page;
    private final int pageSize;
    private final String keyword;
    private final String status;
    private final Long roomId;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    public UtilityBillQuery(int page, int pageSize, String keyword, String status,
                            Long roomId, LocalDate periodStart, LocalDate periodEnd) {
        this.page = page;
        this.pageSize = pageSize;
        this.keyword = keyword;
        this.status = status;
        this.roomId = roomId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public UtilityBillQuery() {
        this(1, 20, null, null, null, null, null);
    }

    public static UtilityBillQuery all() { return new UtilityBillQuery(); }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public String getKeyword() { return keyword; }
    public String getStatus() { return status; }
    public Long getRoomId() { return roomId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
}
