package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;
import java.util.Locale;

/** 比赛维护请求；organizerId 一律由 SessionContext 写入。 */
public final class CompetitionSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String title;
    private final String description;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final LocalDateTime registrationDeadline;
    private final Integer capacity;
    private final String status;

    public CompetitionSaveRequest(Long id, String title, String description,
                                  LocalDateTime startAt, LocalDateTime endAt,
                                  LocalDateTime registrationDeadline, Integer capacity,
                                  String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.registrationDeadline = registrationDeadline;
        this.capacity = capacity;
        this.status = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    public static CompetitionSaveRequest create(String title, String description,
            LocalDateTime startAt, LocalDateTime endAt, LocalDateTime deadline,
            Integer capacity, String status) {
        return new CompetitionSaveRequest(null, title, description, startAt, endAt,
                deadline, capacity, status);
    }

    public static CompetitionSaveRequest update(long id, String title, String description,
            LocalDateTime startAt, LocalDateTime endAt, LocalDateTime deadline,
            Integer capacity, String status) {
        return new CompetitionSaveRequest(Long.valueOf(id), title, description, startAt,
                endAt, deadline, capacity, status);
    }

    public Long getId() { return id; }
    public Long getCompetitionId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getStartTime() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public LocalDateTime getEndTime() { return endAt; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public Integer getCapacity() { return capacity; }
    public String getStatus() { return status; }
    public boolean isUpdate() { return id != null && id.longValue() > 0L; }
}
