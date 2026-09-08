package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** 宿舍公告查询与宿管维护。 */
final class DormAnnouncementService extends DormServiceSupport {
    private final DormRepository repository;

    DormAnnouncementService(DormRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    DormPage<DormAnnouncementDto> list(final SessionContext session, final DormPageQuery query) {
        requireAny(session, Permission.ANNOUNCEMENT_READ, Permission.ANNOUNCEMENT_MANAGE);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        final boolean drafts = session.allows(Permission.ANNOUNCEMENT_MANAGE);
        return execute(new Work<DormPage<DormAnnouncementDto>>() { public DormPage<DormAnnouncementDto> run(java.sql.Connection c) throws Exception { return repository.list(c, q, drafts); } });
    }

    DormAnnouncementDto save(final SessionContext session, final AnnouncementSaveRequest request) {
        require(session, Permission.ANNOUNCEMENT_MANAGE);
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()
                || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new DormException(ResultCodes.INVALID_INPUT, "公告标题和内容不能为空");
        }
        if (request.getExpireAt() != null && request.getPublishAt() != null
                && !request.getExpireAt().isAfter(request.getPublishAt())) {
            throw new DormException(ResultCodes.INVALID_INPUT, "公告过期时间必须晚于发布时间");
        }
        return execute(new Work<DormAnnouncementDto>() { public DormAnnouncementDto run(java.sql.Connection c) throws Exception { return repository.save(c, request, session.getUserId()); } });
    }
}
