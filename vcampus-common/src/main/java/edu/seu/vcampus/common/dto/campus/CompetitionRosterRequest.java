package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;

/** 比赛名单查询载荷。 */
public final class CompetitionRosterRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long competitionId;
    private final CampusPageQuery page;

    public CompetitionRosterRequest(long competitionId, CampusPageQuery page) {
        this.competitionId = competitionId;
        this.page = page == null ? CampusPageQuery.all() : page;
    }

    public long getCompetitionId() { return competitionId; }
    public CampusPageQuery getPage() { return page; }
}
