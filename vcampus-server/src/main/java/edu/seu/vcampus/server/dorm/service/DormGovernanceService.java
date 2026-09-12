package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccessRecordRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionRequest;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnHandleRequest;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepository;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.security.SessionContext;

/** 门禁、未归、卫生及报修治理规则。 */
final class DormGovernanceService extends DormServiceSupport {
    private final DormRepository repository;

    DormGovernanceService(DormRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    AccessRecordDto recordAccess(final SessionContext session, final AccessRecordRequest request) {
        requireAny(session, Permission.DORM_REQUEST, Permission.DORM_GOVERN);
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "门禁参数不能为空");
        final String type = text(request.getRecordType(), "进出类型").toUpperCase();
        final AccessRecordDto value = new AccessRecordDto(0L, session.getUserId(), type,
                request.getOccurredAt(), request.getDoorName(), request.getSource(), request.getNote());
        return execute(new Work<AccessRecordDto>() { public AccessRecordDto run(java.sql.Connection c) throws Exception {
            AccessRecordDto saved = repository.addAccess(c, session.getUserId(), value);
            // 归宿晚于门禁时间（或早于凌晨界限）就当场开一条待处理晚归，让宿管端「未归管理」
            // 里立刻能看到。判定规则和学生端进出列表用的是同一套 DormStayRules，两边不会打架；
            // 同一天重复刷卡只留一条，由仓储层的唯一键兜底。
            org.threeten.bp.LocalTime[] policy = repository.accessPolicy(c);
            if (saved != null && DormStayRules.isLateReturn(saved.getRecordType(), saved.getOccurredAt(), policy[0], policy[1])) {
                repository.openLateAlert(c, session.getUserId(), saved.getOccurredAt().toLocalDate(), saved.getOccurredAt());
            }
            return saved;
        } });
    }

    DormPage<AccessRecordDto> access(final SessionContext session, final DormPageQuery query, final Long studentId) {
        requireAny(session, Permission.DORM_SELF_READ, Permission.DORM_GOVERN);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        final Long owner = session.allows(Permission.DORM_GOVERN) ? studentId : Long.valueOf(session.getUserId());
        return execute(new Work<DormPage<AccessRecordDto>>() { public DormPage<AccessRecordDto> run(java.sql.Connection c) throws Exception { return repository.listAccess(c, owner, q); } });
    }

    DormPage<LateReturnAlertDto> alerts(final SessionContext session, final DormPageQuery query, final Long studentId) {
        requireAny(session, Permission.DORM_SELF_READ, Permission.DORM_GOVERN);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        final Long owner = session.allows(Permission.DORM_GOVERN) ? studentId : Long.valueOf(session.getUserId());
        return execute(new Work<DormPage<LateReturnAlertDto>>() { public DormPage<LateReturnAlertDto> run(java.sql.Connection c) throws Exception { return repository.listAlerts(c, owner, q); } });
    }

    LateReturnAlertDto handleAlert(final SessionContext session, final LateReturnHandleRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "未归处理参数不能为空");
        id(request.getAlertId(), "未归记录");
        return execute(new Work<LateReturnAlertDto>() { public LateReturnAlertDto run(java.sql.Connection c) throws Exception { return repository.handleAlert(c, request.getAlertId(), session.getUserId(), request); } });
    }

    DormPage<HygieneInspectionDto> hygiene(final SessionContext session, final DormPageQuery query) {
        require(session, Permission.DORM_GOVERN);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<DormPage<HygieneInspectionDto>>() { public DormPage<HygieneInspectionDto> run(java.sql.Connection c) throws Exception { return repository.listHygiene(c, q); } });
    }

    HygieneInspectionDto saveHygiene(final SessionContext session, final HygieneInspectionRequest request) {
        require(session, Permission.DORM_GOVERN);
        validateHygiene(request);
        return execute(new Work<HygieneInspectionDto>() { public HygieneInspectionDto run(java.sql.Connection c) throws Exception { return repository.saveHygiene(c, request, session.getUserId()); } });
    }

    DormPage<RepairOrderDto> repairs(final SessionContext session, final DormPageQuery query) {
        requireAny(session, Permission.DORM_REQUEST, Permission.DORM_GOVERN);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        final Long owner = session.allows(Permission.DORM_GOVERN) ? null : Long.valueOf(session.getUserId());
        return execute(new Work<DormPage<RepairOrderDto>>() { public DormPage<RepairOrderDto> run(java.sql.Connection c) throws Exception { return repository.listRepairs(c, owner, q); } });
    }

    RepairOrderDto createRepair(final SessionContext session, final RepairCreateRequest request) {
        require(session, Permission.DORM_REQUEST);
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "报修参数不能为空");
        id(request.getRoomId(), "房间"); text(request.getCategory(), "报修类别"); text(request.getDescription(), "报修描述");
        final String priority = request.getPriority() == null ? "NORMAL" : request.getPriority().trim().toUpperCase();
        return execute(new Work<RepairOrderDto>() {
            @Override public RepairOrderDto run(java.sql.Connection c) throws Exception {
                edu.seu.vcampus.common.dto.dorm.AccommodationDto current = repository.findCurrent(c, session.getUserId(), false);
                if (current == null || current.getRoomId() != request.getRoomId()) throw new DormRepositoryException(ResultCodes.FORBIDDEN, "只能为本人住宿房间报修");
                return repository.createRepair(c, new RepairCreateRequest(request.getRoomId(), request.getCategory().trim(), request.getDescription().trim(), priority), session.getUserId());
            }
        });
    }

    RepairOrderDto updateRepair(final SessionContext session, final RepairStatusRequest request) {
        requireAny(session, Permission.DORM_REQUEST, Permission.DORM_GOVERN);
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "报修状态参数不能为空");
        id(request.getOrderId(), "报修工单");
        final String status = text(request.getStatus(), "报修状态").toUpperCase();
        return execute(new Work<RepairOrderDto>() {
            @Override public RepairOrderDto run(java.sql.Connection c) throws Exception {
                RepairOrderDto old = repository.lockRepair(c, request.getOrderId());
                if (old == null) throw new DormRepositoryException(DormCommands.REPAIR_NOT_FOUND, "报修工单不存在");
                final boolean manager = session.allows(Permission.DORM_GOVERN);
                if (!manager && old.getReporterId() != session.getUserId()) throw new DormRepositoryException(ResultCodes.FORBIDDEN, "无权操作他人报修");
                if (!manager && (!"SUBMITTED".equals(old.getStatus()) || !"CANCELLED".equals(status))) throw new DormRepositoryException(DormCommands.REPAIR_INVALID_STATE, "学生只能取消待处理报修");
                if (manager && !allowed(old.getStatus(), status)) throw new DormRepositoryException(DormCommands.REPAIR_INVALID_STATE, "报修状态流转不合法");
                return repository.updateRepair(c, new RepairStatusRequest(request.getOrderId(), status), session.getUserId());
            }
        });
    }

    RepairOrderDto evaluateRepair(final SessionContext session, final RepairEvaluationRequest request) {
        require(session, Permission.DORM_REQUEST);
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "报修评价参数不能为空");
        id(request.getOrderId(), "报修工单");
        if (request.getScore() < 1 || request.getScore() > 5) {
            throw new DormException(DormCommands.INVALID_INPUT, "评价分数应在 1 到 5 之间");
        }
        if (request.getNote() != null && request.getNote().length() > 500) {
            throw new DormException(DormCommands.INVALID_INPUT, "评价内容长度超限");
        }
        return execute(new Work<RepairOrderDto>() {
            @Override public RepairOrderDto run(java.sql.Connection c) throws Exception {
                RepairOrderDto old = repository.lockRepair(c, request.getOrderId());
                if (old == null) throw new DormRepositoryException(DormCommands.REPAIR_NOT_FOUND, "报修工单不存在");
                if (old.getReporterId() != session.getUserId()) throw new DormRepositoryException(ResultCodes.FORBIDDEN, "只能评价本人报修工单");
                if (!"COMPLETED".equals(old.getStatus()) || old.getEvaluationScore() != null) throw new DormRepositoryException(DormCommands.REPAIR_INVALID_STATE, "报修工单当前不可评价");
                return repository.evaluateRepair(c, request, session.getUserId());
            }
        });
    }

    private static boolean allowed(String old, String next) {
        return ("SUBMITTED".equals(old) && ("ACCEPTED".equals(next) || "CANCELLED".equals(next)))
                || ("ACCEPTED".equals(old) && ("IN_PROGRESS".equals(next) || "CANCELLED".equals(next)))
                || ("IN_PROGRESS".equals(old) && ("COMPLETED".equals(next) || "CANCELLED".equals(next)));
    }

    private static void validateHygiene(HygieneInspectionRequest request) {
        if (request == null) throw new DormException(DormCommands.INVALID_INPUT, "卫生检查参数不能为空");
        id(request.getRoomId(), "房间");
        if (request.getScore() == null || request.getScore().doubleValue() < 0
                || request.getScore().doubleValue() > 100) throw new DormException(DormCommands.INVALID_INPUT, "卫生评分应在 0 到 100 之间");
        text(request.getResult(), "卫生结果");
    }
}
