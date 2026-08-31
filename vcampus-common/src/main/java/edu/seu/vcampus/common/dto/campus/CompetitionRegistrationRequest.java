package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;

/** 报名、取消报名请求；学生身份不从请求读取。 */
public final class CompetitionRegistrationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long competitionId;

    public CompetitionRegistrationRequest(long competitionId) {
        this.competitionId = competitionId;
    }

    public long getCompetitionId() { return competitionId; }
    public long getId() { return competitionId; }
}
