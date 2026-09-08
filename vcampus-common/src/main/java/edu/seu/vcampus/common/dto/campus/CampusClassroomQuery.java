package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;

/** 教室/申请列表查询载荷。 */
public final class CampusClassroomQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final CampusPageQuery page;

    public CampusClassroomQuery(CampusPageQuery page) {
        this.page = page == null ? CampusPageQuery.all() : page;
    }

    public CampusClassroomQuery() { this(null); }
    public CampusPageQuery getPage() { return page; }
}
