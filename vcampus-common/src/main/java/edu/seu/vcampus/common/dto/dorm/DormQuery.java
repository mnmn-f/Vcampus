package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 简单列表筛选，服务端仍会按会话收窄学生数据。 */
public final class DormQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final DormPageQuery page;
    private final Long studentUserId;

    public DormQuery(DormPageQuery page, Long studentUserId) {
        this.page = page == null ? DormPageQuery.all() : page;
        this.studentUserId = studentUserId;
    }

    public DormQuery() { this(null, null); }
    public DormPageQuery getPage() { return page; }
    public Long getStudentUserId() { return studentUserId; }
}
