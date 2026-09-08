package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import edu.seu.vcampus.server.security.SessionContext;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** Hygiene score, inspection detail and recurring task workflow. */
final class DormExtHygieneService extends DormServiceSupport {
    private final DormExtRepository repository;
    DormExtHygieneService(DormExtRepository repository, TransactionManager transactions) { super(transactions); this.repository = repository; }

    HygieneDetailDto submit(SessionContext session, HygieneScoreSubmitRequest request) {
        require(session, Permission.DORM_GOVERN); validate(request);
        return execute(new Work<HygieneDetailDto>() { @Override public HygieneDetailDto run(Connection c) throws Exception {
            LocalDateTime now = LocalDateTime.now(); BigDecimal total = DormHygieneRules.total(request.getItems());
            long id = repository.createInspection(c, request.getRoomId(), session.getUserId(), now, total, DormHygieneRules.result(total), DormHygieneRules.status(total), request.getIssueDescription());
            repository.saveItemScores(c, id, request.getItems()); repository.markTasksDone(c, request.getRoomId(), now.toLocalDate(), id);
            if (DormHygieneRules.needRectify(total)) repository.createTaskIfAbsent(c, request.getRoomId(), HygieneTaskDto.TYPE_RECHECK, DormHygieneRules.recheckDate(now.toLocalDate()), Long.valueOf(id));
            return repository.findInspectionDetail(c, id);
        } });
    }
    HygieneDetailDto detail(SessionContext session, HygieneDetailRequest request) {
        require(session, Permission.DORM_GOVERN); if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "查询参数不能为空"); DormExtValidation.id(request.getInspectionId(), "检查编号");
        return execute(new Work<HygieneDetailDto>() { @Override public HygieneDetailDto run(Connection c) throws Exception { HygieneDetailDto value = repository.findInspectionDetail(c, request.getInspectionId()); if (value == null) throw new DormException(DormExtCommands.HYGIENE_NOT_FOUND, "卫生检查记录不存在"); return value; } });
    }
    HygieneTaskGenerateResultDto generateTasks(SessionContext session, HygieneTaskGenerateRequest request) {
        require(session, Permission.DORM_GOVERN); return execute(new Work<HygieneTaskGenerateResultDto>() { @Override public HygieneTaskGenerateResultDto run(Connection c) throws Exception { return generate(c, request); } });
    }
    HygieneTaskGenerateResultDto generateScheduled(HygieneTaskGenerateRequest request) { return execute(new Work<HygieneTaskGenerateResultDto>() { @Override public HygieneTaskGenerateResultDto run(Connection c) throws Exception { return generate(c, request); } }); }
    DormPage<HygieneTaskDto> tasks(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_GOVERN); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<HygieneTaskDto>>() { @Override public DormPage<HygieneTaskDto> run(Connection c) throws Exception { return repository.listTasks(c, q); } });
    }
    private HygieneTaskGenerateResultDto generate(Connection c, HygieneTaskGenerateRequest request) throws Exception {
        LocalDate date = request == null || request.getPlanDate() == null ? LocalDate.now() : request.getPlanDate(); Long building = request == null ? null : request.getBuildingId(); List<Long> rooms = repository.roomsForWeeklyTask(c, building); int created = 0;
        for (Long room : rooms) if (repository.createTaskIfAbsent(c, room.longValue(), HygieneTaskDto.TYPE_WEEKLY, date, null)) created++;
        return new HygieneTaskGenerateResultDto(date, rooms.size(), created, rooms.size() - created);
    }
    private static void validate(HygieneScoreSubmitRequest request) {
        if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "卫生检查参数不能为空"); DormExtValidation.id(request.getRoomId(), "房间编号");
        try { DormHygieneRules.validate(request.getItems()); } catch (IllegalArgumentException ex) { throw new DormException(DormExtCommands.HYGIENE_ITEMS_INVALID, ex.getMessage()); }
    }
}
