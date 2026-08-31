package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 公告内存专责仓储。 */
final class InMemoryCampusAnnouncementRepository implements CampusAnnouncementRepository {
    private final InMemoryCampusState state;
    InMemoryCampusAnnouncementRepository(InMemoryCampusState state) { this.state = state; }

    @Override public synchronized CampusPage<CampusAnnouncementDto> list(Connection c,
            CampusPageQuery query, String module, String role, boolean drafts) {
        List<CampusAnnouncementDto> rows = new ArrayList<CampusAnnouncementDto>();
        LocalDateTime now = LocalDateTime.now();
        for (CampusAnnouncementDto value : state.announcements.values()) {
            if (module != null && !module.equalsIgnoreCase(value.getModuleCode())) continue;
            if (!drafts && (!("PUBLISHED".equals(value.getStatus()) || "SCHEDULED".equals(value.getStatus()))
                    || !InMemoryCampusSupport.effective(value.getPublishAt(), value.getExpireAt(), now))) continue;
            if ("ROLE".equals(value.getVisibleScope()) && !roleMatches(value, role)) continue;
            if (!InMemoryCampusSupport.matches(query, value.getStatus(), value.getTitle(), value.getContent())) continue;
            rows.add(value);
        }
        return InMemoryCampusSupport.page(rows, query);
    }

    @Override public synchronized CampusAnnouncementDto findAnnouncement(Connection c, long id) {
        return state.announcements.get(Long.valueOf(id));
    }

    @Override public synchronized CampusAnnouncementDto lockAnnouncement(Connection c, long id) {
        return findAnnouncement(c, id);
    }

    @Override public synchronized CampusAnnouncementDto save(Connection c,
            CampusAnnouncementSaveRequest r, long publisher) {
        long id = r.getId() == null ? state.nextAnnouncement++ : r.getId().longValue();
        CampusAnnouncementDto old = state.announcements.get(Long.valueOf(id));
        if (r.getId() != null && old == null) {
            throw new CampusRepositoryException("CAMPUS.ANNOUNCEMENT_NOT_FOUND", "公告不存在");
        }
        CampusAnnouncementDto value = new CampusAnnouncementDto(id, r.getModuleCode(), r.getTitle(),
                r.getContent(), r.getVisibleScope() == null ? "ALL" : r.getVisibleScope(),
                r.getTargetRoleId(), r.getTargetRoleCode(), r.getStatus() == null ? "DRAFT" : r.getStatus(),
                r.getPublishAt(), r.getExpireAt(), old == null ? publisher : old.getPublisherId());
        state.announcements.put(Long.valueOf(id), value);
        return value;
    }

    private boolean roleMatches(CampusAnnouncementDto value, String role) {
        if (role == null) return false;
        if (value.getTargetRoleCode() != null) return role.equalsIgnoreCase(value.getTargetRoleCode());
        return value.getTargetRoleId() != null && value.getTargetRoleId().longValue() == roleOrdinal(role);
    }

    private long roleOrdinal(String role) {
        String[] values = { "STUDENT", "TEACHER", "REGISTRAR", "ACADEMIC_ADMIN", "LIBRARIAN",
                "STORE_MANAGER", "DORM_MANAGER", "AI_KNOWLEDGE_ADMIN", "SYSTEM_ADMIN" };
        for (int i = 0; i < values.length; i++) if (values[i].equalsIgnoreCase(role)) return i + 1L;
        return -1L;
    }
}
