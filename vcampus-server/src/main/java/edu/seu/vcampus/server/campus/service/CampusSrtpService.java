package edu.seu.vcampus.server.campus.service;

import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.campus.repository.CampusRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;

import java.math.BigDecimal;

/** SRTP 项目记录服务；按实际 V1/V2 srtp_records 表实现项目和所属学生。 */
final class CampusSrtpService extends CampusServiceSupport {
    private final CampusRepository repository;

    CampusSrtpService(CampusRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    CampusPage<SrtpRecordDto> mine(final SessionContext session, final CampusPageQuery query) {
        require(session, Permission.SRTP_SELF_READ);
        final CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<CampusPage<SrtpRecordDto>>() { public CampusPage<SrtpRecordDto> run(java.sql.Connection c) throws Exception { return repository.list(c, q, Long.valueOf(session.getUserId())); } });
    }

    CampusPage<SrtpRecordDto> list(final SessionContext session, final CampusPageQuery query) {
        require(session, Permission.SRTP_MANAGE);
        final CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<CampusPage<SrtpRecordDto>>() { public CampusPage<SrtpRecordDto> run(java.sql.Connection c) throws Exception { return repository.list(c, q, null); } });
    }

    SrtpRecordDto save(final SessionContext session, final SrtpSaveRequest request) {
        if (session == null) throw new CampusException("AUTH.UNAUTHORIZED", "请先登录");
        final boolean admin = session.allows(Permission.SRTP_MANAGE);
        if (!admin) require(session, Permission.SRTP_SELF_READ);
        validate(request, admin);
        return execute(new Work<SrtpRecordDto>() {
            @Override public SrtpRecordDto run(java.sql.Connection c) throws Exception {
                SrtpRecordDto old = request.isUpdate() ? repository.lockSrtp(c, request.getId().longValue()) : null;
                if (request.isUpdate() && old == null) throw new CampusException(CampusCommands.SRTP_NOT_FOUND, "SRTP记录不存在");
                if (!admin && old != null && old.getStudentUserId() != session.getUserId()) throw new CampusException(CampusCommands.SRTP_FORBIDDEN, "只能修改本人的SRTP记录");
                if (old != null && !"SUBMITTED".equals(old.getStatus())) throw new CampusException(CampusCommands.SRTP_INVALID_STATE, "当前状态不能修改");
                if (!admin && request.getStatus() != null && !"SUBMITTED".equals(request.getStatus())) throw new CampusException(CampusCommands.SRTP_FORBIDDEN, "学生只能提交本人项目");
                return repository.save(c, request, session.getUserId(), session.getUserId(), admin);
            }
        });
    }

    SrtpRecordDto review(final SessionContext session, final SrtpStatusRequest request) {
        require(session, Permission.SRTP_MANAGE);
        if (request == null) throw new CampusException(CampusCommands.INVALID_INPUT, "审核参数不能为空");
        id(request.getRecordId(), "SRTP记录");
        if (!"APPROVED".equals(request.getStatus()) && !"REJECTED".equals(request.getStatus())
                && !"CANCELLED".equals(request.getStatus())) throw new CampusException(CampusCommands.SRTP_INVALID_STATE, "审核状态不正确");
        return execute(new Work<SrtpRecordDto>() {
            @Override public SrtpRecordDto run(java.sql.Connection c) throws Exception {
                SrtpRecordDto old = repository.lockSrtp(c, request.getRecordId());
                if (old == null) throw new CampusException(CampusCommands.SRTP_NOT_FOUND, "SRTP记录不存在");
                if (!"SUBMITTED".equals(old.getStatus())) throw new CampusException(CampusCommands.SRTP_INVALID_STATE, "该记录已处理");
                return repository.review(c, request, session.getUserId());
            }
        });
    }

    private static void validate(SrtpSaveRequest request, boolean admin) {
        if (request == null) throw new CampusException(CampusCommands.INVALID_INPUT, "SRTP参数不能为空");
        text(request.getProjectCode(), "项目编号");
        text(request.getTitle(), "项目标题");
        BigDecimal credits = request.getCredits();
        if (credits != null && credits.signum() <= 0) throw new CampusException(CampusCommands.INVALID_INPUT, "学分必须为正数");
        if (admin && !request.isUpdate() && (request.getStudentUserId() == null
                || request.getStudentUserId().longValue() <= 0L)) throw new CampusException(CampusCommands.INVALID_INPUT, "项目所属学生不能为空");
        if (admin && request.getStudentUserId() != null && request.getStudentUserId().longValue() <= 0L) {
            throw new CampusException(CampusCommands.INVALID_INPUT, "项目所属学生不正确");
        }
        if (admin && request.getStatus() != null && !"SUBMITTED".equals(request.getStatus())) {
            throw new CampusException(CampusCommands.SRTP_INVALID_STATE, "审核状态请使用审核命令变更");
        }
    }
}
