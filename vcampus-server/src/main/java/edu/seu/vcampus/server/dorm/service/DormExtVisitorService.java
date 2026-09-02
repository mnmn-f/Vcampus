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
import org.threeten.bp.LocalDateTime;

/** Visitor registration and audit workflow. */
final class DormExtVisitorService extends DormServiceSupport {
    private final DormExtRepository repository;
    DormExtVisitorService(DormExtRepository repository, TransactionManager transactions) { super(transactions); this.repository = repository; }

    VisitorRegistrationDto submit(SessionContext session, VisitorRegistrationRequest request) {
        require(session, Permission.DORM_REQUEST); DormExtValidation.visitor(request);
        return execute(new Work<VisitorRegistrationDto>() { @Override public VisitorRegistrationDto run(Connection c) throws Exception {
            Long room = repository.activeRoomOf(c, session.getUserId()); if (room == null) throw new DormException(DormExtCommands.NO_ACCOMMODATION, "没有有效的住宿记录，无法登记来访人员"); return repository.createVisitor(c, session.getUserId(), room.longValue(), request);
        } });
    }
    DormPage<VisitorRegistrationDto> own(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<VisitorRegistrationDto>>() { @Override public DormPage<VisitorRegistrationDto> run(Connection c) throws Exception { return repository.listVisitors(c, q, Long.valueOf(session.getUserId())); } });
    }
    DormPage<VisitorRegistrationDto> list(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_APPROVE); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<VisitorRegistrationDto>>() { @Override public DormPage<VisitorRegistrationDto> run(Connection c) throws Exception { return repository.listVisitors(c, q, null); } });
    }
    VisitorRegistrationDto cancel(SessionContext session, VisitorAuditRequest request) {
        require(session, Permission.DORM_REQUEST); if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "撤销参数不能为空"); DormExtValidation.id(request.getRegistrationId(), "登记编号");
        return execute(new Work<VisitorRegistrationDto>() { @Override public VisitorRegistrationDto run(Connection c) throws Exception { VisitorRegistrationDto current = required(c, request.getRegistrationId()); if (current.getStudentUserId() != session.getUserId()) throw new DormException(ResultCodes.FORBIDDEN, "只能撤销本人提交的登记"); if (!current.isPending()) throw new DormException(DormExtCommands.VISITOR_INVALID_STATE, "只有待审核的登记可以撤销"); return repository.updateVisitorStatus(c, request.getRegistrationId(), VisitorRegistrationDto.STATUS_CANCELLED, null, null, request.getRemark()); } });
    }
    VisitorRegistrationDto audit(SessionContext session, VisitorAuditRequest request) {
        require(session, Permission.DORM_APPROVE); if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "审核参数不能为空"); DormExtValidation.id(request.getRegistrationId(), "登记编号");
        return execute(new Work<VisitorRegistrationDto>() { @Override public VisitorRegistrationDto run(Connection c) throws Exception { VisitorRegistrationDto current = required(c, request.getRegistrationId()); if (!current.isPending()) throw new DormException(DormExtCommands.VISITOR_INVALID_STATE, "该登记已处理，不能重复审核"); String status = request.isApproved() ? VisitorRegistrationDto.STATUS_APPROVED : VisitorRegistrationDto.STATUS_REJECTED; return repository.updateVisitorStatus(c, request.getRegistrationId(), status, Long.valueOf(session.getUserId()), LocalDateTime.now(), request.getRemark()); } });
    }
    private VisitorRegistrationDto required(Connection c, long id) throws Exception { VisitorRegistrationDto value = repository.findVisitor(c, id); if (value == null) throw new DormException(DormExtCommands.VISITOR_NOT_FOUND, "来访登记不存在"); return value; }
}
