package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 请假分页筛选；管理员可使用 studentUserId，学生查询时服务端忽略它。 */
public final class LeaveQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int page;
    private final int pageSize;
    private final String status;
    private final Long studentUserId;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public LeaveQuery() { this(1, 20, null, null, null, null); }

    public LeaveQuery(int page, int pageSize, String status, Long studentUserId,
                      LocalDate startDate, LocalDate endDate) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.page = page; this.pageSize = pageSize; this.status = text(status);
        this.studentUserId = studentUserId; this.startDate = startDate; this.endDate = endDate;
    }

    public LeaveQuery(int page, int pageSize, String status, long studentUserId,
                      LocalDate startDate, LocalDate endDate) {
        this(page, pageSize, status, Long.valueOf(studentUserId), startDate, endDate);
    }

    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public String getStatus() { return status; }
    public Long getStudentUserId() { return studentUserId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
