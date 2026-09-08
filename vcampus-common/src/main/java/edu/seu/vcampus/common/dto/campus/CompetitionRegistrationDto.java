package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 比赛报名记录视图。 */
public final class CompetitionRegistrationDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long competitionId;
    private final long studentUserId;
    private final String status;
    private final LocalDateTime registeredAt;
    private final LocalDateTime cancelledAt;

    public CompetitionRegistrationDto(long competitionId, long studentUserId,
                                      String status, LocalDateTime registeredAt,
                                      LocalDateTime cancelledAt) {
        this.competitionId = competitionId;
        this.studentUserId = studentUserId;
        this.status = status;
        this.registeredAt = registeredAt;
        this.cancelledAt = cancelledAt;
    }

    public long getCompetitionId() { return competitionId; }
    public long getStudentUserId() { return studentUserId; }
    public long getUserId() { return studentUserId; }
    public String getStatus() { return status; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
