package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import edu.seu.vcampus.server.security.SessionContext;
import java.sql.Connection;
import java.util.List;
import org.threeten.bp.LocalDateTime;

/** Scoped dorm-notice workflow and expiry task entry point. */
final class DormExtNoticeService extends DormServiceSupport {
    private final DormExtRepository repository;
    DormExtNoticeService(DormExtRepository repository, TransactionManager transactions) { super(transactions); this.repository = repository; }

    DormPage<NoticeExtraDto> mine(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<NoticeExtraDto>>() { @Override public DormPage<NoticeExtraDto> run(Connection c) throws Exception { return repository.listNotices(c, q, false, repository.activeRoomOf(c, session.getUserId())); } });
    }
    DormPage<NoticeExtraDto> list(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_GOVERN); final DormPageQuery q = query == null ? DormPageQuery.all() : query; DormExtValidation.page(q);
        return execute(new Work<DormPage<NoticeExtraDto>>() { @Override public DormPage<NoticeExtraDto> run(Connection c) throws Exception { return repository.listNotices(c, q, true, null); } });
    }
    NoticeExtraDto save(SessionContext session, NoticeExtraRequest request) {
        require(session, Permission.DORM_GOVERN); validate(request);
        return execute(new Work<NoticeExtraDto>() { @Override public NoticeExtraDto run(Connection c) throws Exception { if (repository.findNotice(c, request.getAnnouncementId()) == null) throw new DormException(DormExtCommands.NOTICE_NOT_FOUND, "宿舍公告不存在"); return repository.saveNoticeExtra(c, request, session.getUserId()); } });
    }
    int expire(LocalDateTime now) {
        Integer result = execute(new Work<Integer>() { @Override public Integer run(Connection c) throws Exception { return Integer.valueOf(repository.expireDormAnnouncements(c, now)); } });
        return result == null ? 0 : result.intValue();
    }
    private static void validate(NoticeExtraRequest request) {
        if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "公告设置参数不能为空"); DormExtValidation.id(request.getAnnouncementId(), "公告编号");
        String type = request.getNoticeType(); if (!(NoticeExtraDto.TYPE_GENERAL.equals(type) || NoticeExtraDto.TYPE_MAINTENANCE.equals(type) || NoticeExtraDto.TYPE_HYGIENE.equals(type) || NoticeExtraDto.TYPE_SAFETY.equals(type) || NoticeExtraDto.TYPE_URGENT.equals(type))) throw new DormException(DormExtCommands.INVALID_INPUT, "公告类型不正确");
        String scope = request.getScopeType();
        if (NoticeExtraDto.SCOPE_ALL.equals(scope)) { if (request.getScopeBuildingId() != null || request.getScopeRoomId() != null) throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID, "面向全体的公告不能指定楼栋或房间"); return; }
        if (NoticeExtraDto.SCOPE_BUILDING.equals(scope)) { if (request.getScopeBuildingId() == null) throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID, "按楼栋投放时必须指定楼栋"); if (request.getScopeRoomId() != null) throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID, "按楼栋投放时不能同时指定房间"); DormExtValidation.id(request.getScopeBuildingId().longValue(), "楼栋编号"); return; }
        if (NoticeExtraDto.SCOPE_ROOM.equals(scope)) { if (request.getScopeRoomId() == null) throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID, "按房间投放时必须指定房间"); DormExtValidation.id(request.getScopeRoomId().longValue(), "房间编号"); return; }
        throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID, "公告可见范围不正确");
    }
}
