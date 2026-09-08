package edu.seu.vcampus.server.campus.service;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementQuery;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.campus.repository.CampusRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;

import org.threeten.bp.LocalDateTime;

/** 公告按角色可见、按生效时间过滤，并由所属管理员发布或撤回。 */
final class CampusAnnouncementService extends CampusServiceSupport {
    private final CampusRepository repository;

    CampusAnnouncementService(CampusRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    CampusPage<CampusAnnouncementDto> list(final SessionContext session, final CampusAnnouncementQuery request) {
        requireAny(session, Permission.ANNOUNCEMENT_READ, Permission.ANNOUNCEMENT_MANAGE);
        final CampusAnnouncementQuery value = request == null ? new CampusAnnouncementQuery() : request;
        final CampusPageQuery query = value.getPage();
        page(query.getPage(), query.getPageSize());
        final String module = value.getModuleCode() == null ? query.getModuleCode() : value.getModuleCode();
        return execute(new Work<CampusPage<CampusAnnouncementDto>>() { public CampusPage<CampusAnnouncementDto> run(java.sql.Connection c) throws Exception { return repository.list(c, query, module, session.getActiveRole().name(), session.allows(Permission.ANNOUNCEMENT_MANAGE)); } });
    }

    CampusAnnouncementDto save(final SessionContext session, final CampusAnnouncementSaveRequest request) {
        require(session, Permission.ANNOUNCEMENT_MANAGE);
        validate(request);
        requireOwnedModule(session, request.getModuleCode());
        if (request.isUpdate()) {
            id(request.getId().longValue(), "公告");
        }
        return execute(new Work<CampusAnnouncementDto>() {
            @Override public CampusAnnouncementDto run(java.sql.Connection c) throws Exception {
                if (request.isUpdate()) {
                    CampusAnnouncementDto old = repository.lockAnnouncement(c, request.getId().longValue());
                    if (old == null) throw new CampusException(CampusCommands.ANNOUNCEMENT_NOT_FOUND, "公告不存在");
                    if ("REVOKED".equals(old.getStatus())) throw new CampusException(CampusCommands.ANNOUNCEMENT_INVALID_STATE, "已撤回公告不能修改");
                }
                return repository.save(c, request, session.getUserId());
            }
        });
    }

    CampusAnnouncementDto revoke(final SessionContext session, final long id) {
        require(session, Permission.ANNOUNCEMENT_MANAGE);
        id(id, "公告");
        return execute(new Work<CampusAnnouncementDto>() {
            @Override public CampusAnnouncementDto run(java.sql.Connection c) throws Exception {
                CampusAnnouncementDto old = repository.lockAnnouncement(c, id);
                if (old == null) throw new CampusException(CampusCommands.ANNOUNCEMENT_NOT_FOUND, "公告不存在");
                requireOwnedModule(session, old.getModuleCode());
                if ("REVOKED".equals(old.getStatus()) || "EXPIRED".equals(old.getStatus())) throw new CampusException(CampusCommands.ANNOUNCEMENT_INVALID_STATE, "公告已不可撤回");
                CampusAnnouncementSaveRequest request = new CampusAnnouncementSaveRequest(Long.valueOf(id), old.getModuleCode(), old.getTitle(), old.getContent(), old.getVisibleScope(), old.getTargetRoleId(), old.getTargetRoleCode(), "REVOKED", old.getPublishAt(), old.getExpireAt());
                return repository.save(c, request, session.getUserId());
            }
        });
    }

    private static void validate(CampusAnnouncementSaveRequest request) {
        if (request == null) throw new CampusException(CampusCommands.INVALID_INPUT, "公告参数不能为空");
        String module = text(request.getModuleCode(), "公告模块").toUpperCase();
        if (!"SYSTEM".equals(module) && !"ACADEMIC".equals(module) && !"LIBRARY".equals(module)
                && !"DORM".equals(module)) throw new CampusException(CampusCommands.INVALID_INPUT, "公告模块不正确");
        text(request.getTitle(), "公告标题");
        text(request.getContent(), "公告内容");
        String scope = request.getVisibleScope() == null ? "ALL" : request.getVisibleScope();
        if (!"ALL".equals(scope) && !"ROLE".equals(scope)) {
            throw new CampusException(CampusCommands.INVALID_INPUT, "公告可见范围不正确");
        }
        if ("ROLE".equals(scope) && request.getTargetRoleId() == null
                && request.getTargetRoleCode() == null) throw new CampusException(CampusCommands.ANNOUNCEMENT_VISIBILITY, "角色公告缺少目标角色");
        String status = request.getStatus() == null ? "DRAFT" : request.getStatus();
        if (!"DRAFT".equals(status) && !"SCHEDULED".equals(status) && !"PUBLISHED".equals(status)
                && !"REVOKED".equals(status)) throw new CampusException(CampusCommands.ANNOUNCEMENT_INVALID_STATE, "公告状态不正确");
        LocalDateTime publish = request.getPublishAt();
        LocalDateTime expire = request.getExpireAt();
        if (publish != null && expire != null && !expire.isAfter(publish)) {
            throw new CampusException(CampusCommands.INVALID_INPUT, "公告失效时间必须晚于生效时间");
        }
    }

    static void requireOwnedModule(SessionContext session, String module) {
        Role role = session.getActiveRole();
        if (role == Role.ACADEMIC_ADMIN && !"ACADEMIC".equals(module)) {
            throw new CampusException(CampusCommands.ANNOUNCEMENT_VISIBILITY, "教务管理员只能维护教务公告");
        }
        if (role == Role.LIBRARIAN && !"LIBRARY".equals(module)) {
            throw new CampusException(CampusCommands.ANNOUNCEMENT_VISIBILITY, "图书管理员只能维护图书馆公告");
        }
        if (role == Role.DORM_MANAGER && !"DORM".equals(module)) {
            throw new CampusException(CampusCommands.ANNOUNCEMENT_VISIBILITY, "宿管只能维护宿舍公告");
        }
    }
}
