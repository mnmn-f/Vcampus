package edu.seu.vcampus.server.campus.service;

import edu.seu.vcampus.common.dto.campus.CampusCompetitionQuery;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.campus.repository.CampusRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;

import org.threeten.bp.LocalDateTime;

/** 比赛维护、报名和报名名单；报名在锁定比赛行的事务内完成。 */
final class CampusCompetitionService extends CampusServiceSupport {
    private final CampusRepository repository;

    CampusCompetitionService(CampusRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    CampusPage<CompetitionDto> list(final SessionContext session, final CampusCompetitionQuery request) {
        requireAny(session, Permission.COMPETITION_ENROLL, Permission.COMPETITION_MANAGE);
        final CampusCompetitionQuery value = request == null ? new CampusCompetitionQuery() : request;
        final CampusPageQuery query = value.getPage();
        page(query.getPage(), query.getPageSize());
        return execute(new Work<CampusPage<CompetitionDto>>() { public CampusPage<CompetitionDto> run(java.sql.Connection c) throws Exception { return repository.list(c, query, session.allows(Permission.COMPETITION_MANAGE)); } });
    }

    CompetitionDto save(final SessionContext session, final CompetitionSaveRequest request) {
        require(session, Permission.COMPETITION_MANAGE);
        validate(request);
        if (request.getId() != null && request.getId().longValue() <= 0L) {
            throw new CampusException(CampusCommands.INVALID_INPUT, "比赛编号不正确");
        }
        return execute(new Work<CompetitionDto>() {
            @Override public CompetitionDto run(java.sql.Connection c) throws Exception {
                if (request.isUpdate()) {
                    CompetitionDto old = repository.lockCompetition(c, request.getId().longValue());
                    if (old == null) throw new CampusException(CampusCommands.COMPETITION_NOT_FOUND, "比赛不存在");
                    if ("CANCELLED".equals(old.getStatus())) throw new CampusException(CampusCommands.COMPETITION_INVALID_STATE, "已取消比赛不能修改");
                    if (request.getCapacity() != null && request.getCapacity().intValue() < old.getRegisteredCount()) throw new CampusException(CampusCommands.COMPETITION_INVALID_STATE, "容量不能小于已报名人数");
                }
                return repository.save(c, request, session.getUserId());
            }
        });
    }

    CompetitionRegistrationDto register(final SessionContext session, final long competitionId) {
        require(session, Permission.COMPETITION_ENROLL);
        id(competitionId, "比赛");
        return execute(new Work<CompetitionRegistrationDto>() {
            @Override public CompetitionRegistrationDto run(java.sql.Connection c) throws Exception {
                CompetitionDto competition = repository.lockCompetition(c, competitionId);
                if (competition == null) throw new CampusException(CampusCommands.COMPETITION_NOT_FOUND, "比赛不存在");
                LocalDateTime now = LocalDateTime.now();
                if (!"PUBLISHED".equals(competition.getStatus())) throw new CampusException(CampusCommands.COMPETITION_INVALID_STATE, "比赛当前不可报名");
                if (competition.getRegistrationDeadline() == null || !now.isBefore(competition.getRegistrationDeadline()) || !now.isBefore(competition.getStartAt())) throw new CampusException(CampusCommands.COMPETITION_WINDOW_CLOSED, "报名时间已截止");
                CompetitionRegistrationDto old = repository.findRegistration(c, competitionId, session.getUserId(), true);
                if (old != null && "REGISTERED".equals(old.getStatus())) throw new CampusException(CampusCommands.COMPETITION_DUPLICATE, "已报名该比赛");
                if (competition.getCapacity() != null && repository.countRegistered(c, competitionId) >= competition.getCapacity().intValue()) throw new CampusException(CampusCommands.COMPETITION_FULL, "比赛报名人数已满");
                return repository.register(c, competitionId, session.getUserId());
            }
        });
    }

    void cancel(final SessionContext session, final long competitionId) {
        require(session, Permission.COMPETITION_ENROLL);
        id(competitionId, "比赛");
        execute(new Work<Object>() {
            @Override public Object run(java.sql.Connection c) throws Exception {
                CompetitionDto competition = repository.lockCompetition(c, competitionId);
                if (competition == null) throw new CampusException(CampusCommands.COMPETITION_NOT_FOUND, "比赛不存在");
                if (!"PUBLISHED".equals(competition.getStatus()) || !LocalDateTime.now().isBefore(competition.getStartAt()) || !LocalDateTime.now().isBefore(competition.getRegistrationDeadline())) throw new CampusException(CampusCommands.COMPETITION_INVALID_STATE, "当前不能取消报名");
                repository.cancelRegistration(c, competitionId, session.getUserId());
                return null;
            }
        });
    }

    CampusPage<CompetitionRegistrationDto> roster(final SessionContext session, final long competitionId,
                                                   final CampusPageQuery query) {
        require(session, Permission.COMPETITION_MANAGE);
        id(competitionId, "比赛");
        final CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<CampusPage<CompetitionRegistrationDto>>() {
            @Override public CampusPage<CompetitionRegistrationDto> run(java.sql.Connection c) throws Exception {
                if (repository.findCompetition(c, competitionId) == null) throw new CampusException(CampusCommands.COMPETITION_NOT_FOUND, "比赛不存在");
                return repository.roster(c, competitionId, q);
            }
        });
    }

    CampusPage<CompetitionRegistrationDto> mine(final SessionContext session, CampusPageQuery query) {
        require(session, Permission.COMPETITION_ENROLL);
        final CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<CampusPage<CompetitionRegistrationDto>>() {
            @Override public CampusPage<CompetitionRegistrationDto> run(java.sql.Connection c) throws Exception {
                return repository.registrationsForStudent(c, session.getUserId(), q);
            }
        });
    }

    private static void validate(CompetitionSaveRequest request) {
        if (request == null) throw new CampusException(CampusCommands.INVALID_INPUT, "比赛参数不能为空");
        text(request.getTitle(), "比赛标题");
        if (request.getStartAt() == null || request.getEndAt() == null
                || request.getRegistrationDeadline() == null) throw new CampusException(CampusCommands.INVALID_INPUT, "比赛时间不能为空");
        if (!request.getEndAt().isAfter(request.getStartAt())
                || request.getRegistrationDeadline().isAfter(request.getStartAt())) throw new CampusException(CampusCommands.INVALID_INPUT, "比赛时间范围不正确");
        if (request.getCapacity() != null && request.getCapacity().intValue() <= 0) {
            throw new CampusException(CampusCommands.INVALID_INPUT, "比赛容量必须为正数");
        }
        String status = request.getStatus() == null ? "DRAFT" : request.getStatus();
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status)
                && !"CLOSED".equals(status) && !"CANCELLED".equals(status)) {
            throw new CampusException(CampusCommands.COMPETITION_INVALID_STATE, "比赛状态不正确");
        }
    }
}
