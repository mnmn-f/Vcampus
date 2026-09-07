package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import edu.seu.vcampus.server.security.SessionContext;
import java.sql.Connection;
import java.util.*;
import org.threeten.bp.LocalDate;

/** Stay status, access policy, repair-entry permit and room deletion workflow. */
final class DormExtAccessService extends DormServiceSupport {
    private final DormExtRepository repository;
    DormExtAccessService(DormExtRepository repository, TransactionManager transactions) { super(transactions); this.repository = repository; }

    StayStatusDto myStay(SessionContext session) {
        require(session, Permission.DORM_SELF_READ);
        return execute(new Work<StayStatusDto>() { @Override public StayStatusDto run(Connection c) throws Exception { List<StayStatusDto> rows = resolve(c, Long.valueOf(session.getUserId())); if (rows.isEmpty()) throw new DormException(DormExtCommands.NO_ACCOMMODATION, "没有有效的住宿记录"); return rows.get(0); } });
    }
    DormPage<StayStatusDto> stays(SessionContext session) {
        require(session, Permission.DORM_GOVERN);
        return execute(new Work<DormPage<StayStatusDto>>() { @Override public DormPage<StayStatusDto> run(Connection c) throws Exception { List<StayStatusDto> rows = resolve(c, null); return new DormPage<StayStatusDto>(1, Math.max(rows.size(), 1), rows.size(), rows); } });
    }
    DormPage<AccessRecordExtDto> access(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<AccessRecordExtDto>>() { @Override public DormPage<AccessRecordExtDto> run(Connection c) throws Exception { return repository.listAccessRecords(c, session.getUserId(), q, repository.loadAccessPolicy(c)); } });
    }
    AccessPolicyDto policy(SessionContext session) { require(session, Permission.DORM_GOVERN); return execute(new Work<AccessPolicyDto>() { @Override public AccessPolicyDto run(Connection c) throws Exception { return repository.loadAccessPolicy(c); } }); }
    AccessPolicyDto savePolicy(SessionContext session, AccessPolicyRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null || request.getCurfewTime() == null || request.getDawnTime() == null) throw new DormException(DormExtCommands.POLICY_INVALID, "门禁时间和清晨时间都不能为空");
        if (!request.getDawnTime().isBefore(request.getCurfewTime())) throw new DormException(DormExtCommands.POLICY_INVALID, "清晨时间必须早于门禁时间，否则整天都会被判成晚归");
        return execute(new Work<AccessPolicyDto>() { @Override public AccessPolicyDto run(Connection c) throws Exception { return repository.saveAccessPolicy(c, request, session.getUserId()); } });
    }
    RepairEntryPermitDto setPermit(SessionContext session, RepairEntryPermitRequest request) {
        require(session, Permission.DORM_REQUEST); if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "授权参数不能为空"); DormExtValidation.id(request.getRepairOrderId(), "工单号");
        return execute(new Work<RepairEntryPermitDto>() { @Override public RepairEntryPermitDto run(Connection c) throws Exception {
            Long reporter = repository.repairReporterOf(c, request.getRepairOrderId()); if (reporter == null) throw new DormException(DormExtCommands.REPAIR_NOT_FOUND, "报修单不存在");
            if (reporter.longValue() != session.getUserId()) throw new DormException(ResultCodes.FORBIDDEN, "只能为本人提交的报修单授权");
            if (request.isAllowEnter() && DormExtValidation.blank(repository.reporterPhoneOf(c, request.getRepairOrderId())) && DormExtValidation.blank(request.getNote())) throw new DormException(DormExtCommands.CONTACT_REQUIRED, "账号里没有登记手机号，请在备注里留下维修期间能联系到你的方式");
            return repository.saveRepairPermit(c, request, session.getUserId());
        } });
    }
    DormPage<RepairEntryPermitDto> permits(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<RepairEntryPermitDto>>() { @Override public DormPage<RepairEntryPermitDto> run(Connection c) throws Exception { return repository.listRepairPermits(c, session.getUserId(), q); } });
    }
    Long deleteRoom(SessionContext session, RoomDeleteRequest request) {
        require(session, Permission.DORM_MANAGE); if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "删除参数不能为空"); DormExtValidation.id(request.getRoomId(), "房间编号");
        return execute(new Work<Long>() { @Override public Long run(Connection c) throws Exception { if (repository.activeResidentCount(c, request.getRoomId()) > 0) throw new DormException(DormExtCommands.ROOM_OCCUPIED, "房间仍有在住学生，不能删除"); if (repository.roomReferenceCount(c, request.getRoomId()) > 0) throw new DormException(DormExtCommands.ROOM_HAS_HISTORY, "房间已有历史业务数据，不能删除；如需停用请把房间状态改为已关闭"); repository.deleteRoom(c, request.getRoomId()); return Long.valueOf(request.getRoomId()); } });
    }
    private List<StayStatusDto> resolve(Connection c, Long student) throws Exception {
        List<StayStatusDto> rows = repository.stayStatusRows(c, student); List<StayStatusDto> result = new ArrayList<StayStatusDto>(rows.size());
        for (StayStatusDto row : rows) { boolean onLeave = repository.hasApprovedLeave(c, row.getStudentUserId(), LocalDate.now()); result.add(new StayStatusDto(row.getStudentUserId(), row.getRoomId(), row.getBuildingCode(), row.getRoomNo(), DormStayRules.status(row.getLastExitAt(), row.getLastEntryAt(), onLeave), row.getLastExitAt(), row.getLastEntryAt())); }
        return result;
    }
}
