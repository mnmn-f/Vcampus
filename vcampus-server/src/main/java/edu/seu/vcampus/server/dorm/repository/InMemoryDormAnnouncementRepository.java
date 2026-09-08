package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.protocol.command.DormCommands;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/** 内存宿舍公告仓储。 */
final class InMemoryDormAnnouncementRepository implements DormAnnouncementRepository {
    private final InMemoryDormState state;

    InMemoryDormAnnouncementRepository(InMemoryDormState state) { this.state = state; }

    @Override
    public synchronized DormPage<DormAnnouncementDto> list(Connection c, DormPageQuery q,
                                                            boolean includeDrafts) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<DormAnnouncementDto> rows = new ArrayList<DormAnnouncementDto>();
        for (DormAnnouncementDto item : state.announcements.values()) {
            if ((includeDrafts || "PUBLISHED".equals(item.getStatus()))
                    && InMemoryDormSupport.status(query.getStatus(), item.getStatus())
                    && InMemoryDormSupport.matches(query.getKeyword(), item.getTitle(), item.getContent())) {
                rows.add(item);
            }
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized DormAnnouncementDto save(Connection c, AnnouncementSaveRequest request,
                                                 long publisherId) {
        long id = request.getId() <= 0 ? state.nextAnnouncement++ : request.getId();
        if (request.getId() > 0 && !state.announcements.containsKey(Long.valueOf(id))) {
            throw new DormRepositoryException(DormCommands.REQUEST_NOT_FOUND, "公告不存在");
        }
        String status = request.getStatus() == null ? "DRAFT" : request.getStatus();
        DormAnnouncementDto value = new DormAnnouncementDto(id, request.getTitle(), request.getContent(),
                request.getVisibleScope() == null ? "ALL" : request.getVisibleScope(),
                request.getTargetRoleId(), status, request.getPublishAt(), request.getExpireAt(), publisherId);
        state.announcements.put(Long.valueOf(id), value);
        return value;
    }
}
