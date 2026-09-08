package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 校园比赛视图。 */
public final class CompetitionDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String title;
    private final String description;
    private final long organizerId;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final LocalDateTime registrationDeadline;
    private final Integer capacity;
    private final String status;
    private final long registeredCount;

    public CompetitionDto(long id, String title, String description, long organizerId,
                          LocalDateTime startAt, LocalDateTime endAt,
                          LocalDateTime registrationDeadline, Integer capacity,
                          String status, long registeredCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.organizerId = organizerId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.registrationDeadline = registrationDeadline;
        this.capacity = capacity;
        this.status = status;
        this.registeredCount = registeredCount;
    }

    public long getId() { return id; }
    public long getCompetitionId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getOrganizerId() { return organizerId; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getStartTime() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public LocalDateTime getEndTime() { return endAt; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public Integer getCapacity() { return capacity; }
    public String getStatus() { return status; }
    public long getRegisteredCount() { return registeredCount; }
    public long getRegistrationCount() { return registeredCount; }
}
