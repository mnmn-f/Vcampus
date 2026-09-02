package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import java.util.*;
import org.threeten.bp.LocalDateTime;

/** In-memory dorm notice and scheduler support persistence. */
final class InMemoryDormNoticeStore {
    private final InMemoryDormExtState state;
    InMemoryDormNoticeStore(InMemoryDormExtState state) { this.state = state; }

    void addBuilding(long id, String code) { state.buildings.put(Long.valueOf(id), code); }
    void setRoomBuilding(long roomId, long buildingId) { state.roomBuildings.put(Long.valueOf(roomId), Long.valueOf(buildingId)); }

    void addNotice(long id, String title, String status) {
        state.notices.put(Long.valueOf(id), new NoticeExtraDto(id, title, title + " 正文", status,
                LocalDateTime.now().minusDays(1), null, 90L, NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ALL,
                null, null, null, null, false, null));
    }

    DormPage<NoticeExtraDto> list(DormPageQuery query, boolean manage, Long roomId) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<NoticeExtraDto> rows = new ArrayList<NoticeExtraDto>();
        for (NoticeExtraDto item : state.notices.values()) {
            if (!manage && (!"PUBLISHED".equals(item.getStatus()) || !visible(item, roomId))) continue;
            if (!InMemoryDormSupport.matches(q.getKeyword(), item.getTitle(), item.getContent())) continue;
            String filter = q.getStatus() == null ? null : q.getStatus().trim();
            if (filter != null && !filter.isEmpty()) {
                String actual = manage ? item.getStatus() : item.getNoticeType();
                if (!filter.equals(actual)) continue;
            }
            rows.add(item);
        }
        Collections.sort(rows, new Comparator<NoticeExtraDto>() {
            @Override public int compare(NoticeExtraDto a, NoticeExtraDto b) {
                if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
                return Long.valueOf(b.getAnnouncementId()).compareTo(Long.valueOf(a.getAnnouncementId()));
            }
        });
        return InMemoryDormSupport.page(rows, q);
    }

    NoticeExtraDto find(long id) { return state.notices.get(Long.valueOf(id)); }

    NoticeExtraDto save(NoticeExtraRequest request) {
        Long key = Long.valueOf(request.getAnnouncementId());
        NoticeExtraDto old = state.notices.get(key);
        if (old == null) throw new DormRepositoryException(DormExtCommands.NOTICE_NOT_FOUND, "宿舍公告不存在");
        Long buildingId = request.getScopeBuildingId();
        Long roomId = request.getScopeRoomId();
        if (roomId != null && buildingId == null) buildingId = state.roomBuildings.get(roomId);
        String buildingCode = buildingId == null ? null : state.buildings.get(buildingId);
        String roomNo = roomId == null ? null : InMemoryDormSupport.location(state.rooms, roomId.longValue())[1];
        LocalDateTime pinnedAt = !request.isPinned() ? null : old.isPinned() && old.getPinnedAt() != null
                ? old.getPinnedAt() : LocalDateTime.now();
        NoticeExtraDto value = new NoticeExtraDto(old.getAnnouncementId(), old.getTitle(), old.getContent(), old.getStatus(),
                old.getPublishAt(), old.getExpireAt(), old.getPublisherId(), request.getNoticeType(), request.getScopeType(),
                buildingId, roomId, buildingCode, roomNo, request.isPinned(), pinnedAt);
        state.notices.put(key, value);
        return value;
    }

    void setExpirable(int count) { state.expirableAnnouncements = Math.max(0, count); }
    int expire() { int result = state.expirableAnnouncements; state.expirableAnnouncements = 0; return result; }

    private boolean visible(NoticeExtraDto item, Long roomId) {
        if (NoticeExtraDto.SCOPE_ALL.equals(item.getScopeType())) return true;
        if (roomId == null) return false;
        if (NoticeExtraDto.SCOPE_ROOM.equals(item.getScopeType())) return roomId.equals(item.getScopeRoomId());
        Long building = state.roomBuildings.get(roomId);
        return building != null && building.equals(item.getScopeBuildingId());
    }
}
