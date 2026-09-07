package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 比赛和报名内存专责仓储。 */
final class InMemoryCampusCompetitionRepository implements CampusCompetitionRepository {
    private final InMemoryCampusState state;
    InMemoryCampusCompetitionRepository(InMemoryCampusState state) { this.state = state; }

    @Override public synchronized CampusPage<CompetitionDto> list(Connection c, CampusPageQuery q,
            boolean drafts) {
        List<CompetitionDto> rows = new ArrayList<CompetitionDto>();
        for (CompetitionDto value : state.competitions.values()) {
            if (!drafts && !"PUBLISHED".equals(value.getStatus())) continue;
            if (!InMemoryCampusSupport.matches(q, value.getStatus(), value.getTitle(), value.getDescription())) continue;
            rows.add(withCount(value));
        }
        return InMemoryCampusSupport.page(rows, q);
    }

    @Override public synchronized CompetitionDto findCompetition(Connection c, long id) {
        CompetitionDto value = state.competitions.get(Long.valueOf(id));
        return value == null ? null : withCount(value);
    }

    @Override public synchronized CompetitionDto lockCompetition(Connection c, long id) {
        return findCompetition(c, id);
    }

    @Override public synchronized CompetitionDto save(Connection c, CompetitionSaveRequest r,
            long actor) {
        long id = r.getId() == null ? state.nextCompetition++ : r.getId().longValue();
        CompetitionDto old = state.competitions.get(Long.valueOf(id));
        if (r.getId() != null && old == null) {
            throw new CampusRepositoryException("CAMPUS.COMPETITION_NOT_FOUND", "比赛不存在");
        }
        long organizer = old == null ? actor : old.getOrganizerId();
        CompetitionDto value = new CompetitionDto(id, r.getTitle(), r.getDescription(), organizer,
                r.getStartAt(), r.getEndAt(), r.getRegistrationDeadline(), r.getCapacity(),
                r.getStatus() == null ? "DRAFT" : r.getStatus(), 0L);
        state.competitions.put(Long.valueOf(id), value);
        return withCount(value);
    }

    @Override public synchronized CompetitionRegistrationDto findRegistration(Connection c,
            long competitionId, long studentId, boolean forUpdate) {
        return state.registrations.get(key(competitionId, studentId));
    }

    @Override public synchronized long countRegistered(Connection c, long competitionId) {
        long count = 0L;
        for (CompetitionRegistrationDto value : state.registrations.values()) {
            if (value.getCompetitionId() == competitionId && "REGISTERED".equals(value.getStatus())) count++;
        }
        return count;
    }

    @Override public synchronized CompetitionRegistrationDto register(Connection c, long competitionId,
            long studentId) {
        String key = key(competitionId, studentId);
        CompetitionRegistrationDto old = state.registrations.get(key);
        LocalDateTime now = LocalDateTime.now();
        if (old != null && "REGISTERED".equals(old.getStatus())) {
            throw new CampusRepositoryException("CAMPUS.COMPETITION_DUPLICATE", "已报名该比赛");
        }
        CompetitionRegistrationDto value = new CompetitionRegistrationDto(competitionId, studentId,
                "REGISTERED", now, null);
        state.registrations.put(key, value);
        return value;
    }

    @Override public synchronized void cancelRegistration(Connection c, long competitionId, long studentId) {
        String key = key(competitionId, studentId);
        CompetitionRegistrationDto old = state.registrations.get(key);
        if (old == null || !"REGISTERED".equals(old.getStatus())) {
            throw new CampusRepositoryException("CAMPUS.COMPETITION_INVALID_STATE", "报名不在可取消状态");
        }
        state.registrations.put(key, new CompetitionRegistrationDto(competitionId, studentId,
                "CANCELLED", old.getRegisteredAt(), LocalDateTime.now()));
    }

    @Override public synchronized CampusPage<CompetitionRegistrationDto> roster(Connection c,
            long competitionId, CampusPageQuery q) {
        List<CompetitionRegistrationDto> rows = new ArrayList<CompetitionRegistrationDto>();
        for (CompetitionRegistrationDto value : state.registrations.values()) {
            if (value.getCompetitionId() == competitionId
                    && InMemoryCampusSupport.matches(q, value.getStatus(), String.valueOf(value.getStudentUserId()))) rows.add(value);
        }
        return InMemoryCampusSupport.page(rows, q);
    }

    private CompetitionDto withCount(CompetitionDto value) {
        return new CompetitionDto(value.getId(), value.getTitle(), value.getDescription(),
                value.getOrganizerId(), value.getStartAt(), value.getEndAt(),
                value.getRegistrationDeadline(), value.getCapacity(), value.getStatus(),
                countRegistered(null, value.getId()));
    }

    private static String key(long competitionId, long studentId) {
        return competitionId + ":" + studentId;
    }
}
