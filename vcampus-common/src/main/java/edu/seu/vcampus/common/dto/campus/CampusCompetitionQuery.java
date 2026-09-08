package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;

/** 比赛分页查询载荷；学生身份不从请求读取。 */
public final class CampusCompetitionQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final CampusPageQuery page;

    public CampusCompetitionQuery(CampusPageQuery page) {
        this.page = page == null ? CampusPageQuery.all() : page;
    }

    public CampusCompetitionQuery() { this(null); }
    public CampusPageQuery getPage() { return page; }
}
