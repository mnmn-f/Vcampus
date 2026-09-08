package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import java.util.*;
import org.threeten.bp.LocalDateTime;

/** In-memory visitor-registration persistence. */
final class InMemoryDormVisitorStore {
    private final InMemoryDormExtState state;
    InMemoryDormVisitorStore(InMemoryDormExtState state) { this.state = state; }

    void addAccommodation(long studentId, long roomId) { state.activeRooms.put(Long.valueOf(studentId), Long.valueOf(roomId)); }
    Long activeRoom(long studentId) { return state.activeRooms.get(Long.valueOf(studentId)); }

    VisitorRegistrationDto create(long studentId, long roomId, VisitorRegistrationRequest request) {
        long id = state.nextVisitorId++;
        String[] parts = InMemoryDormSupport.location(state.rooms, roomId);
        VisitorRegistrationDto value = new VisitorRegistrationDto(id, studentId, roomId, parts[0], parts[1],
                request.getVisitorName().trim(), VisitorRegistrationDto.mask(request.getVisitorIdCard()),
                request.getVisitorPhone(), request.getVisitReason().trim(), request.getStartAt(), request.getEndAt(),
                LocalDateTime.now(), VisitorRegistrationDto.STATUS_PENDING, null, null, null);
        state.visitors.put(Long.valueOf(id), value);
        return value;
    }

    DormPage<VisitorRegistrationDto> list(DormPageQuery query, Long studentId) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<VisitorRegistrationDto> rows = new ArrayList<VisitorRegistrationDto>();
        for (VisitorRegistrationDto item : state.visitors.values()) {
            boolean mine = studentId == null || studentId.longValue() == item.getStudentUserId();
            if (mine && InMemoryDormSupport.status(q.getStatus(), item.getAuditStatus())
                    && InMemoryDormSupport.matches(q.getKeyword(), item.getVisitorName(), item.getVisitReason(), item.getRoomNo())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, q);
    }

    VisitorRegistrationDto find(long id) { return state.visitors.get(Long.valueOf(id)); }

    VisitorRegistrationDto update(long id, String status, Long auditorId, LocalDateTime auditedAt, String remark) {
        VisitorRegistrationDto old = find(id);
        if (old == null) throw new DormRepositoryException(DormExtCommands.VISITOR_NOT_FOUND, "来访登记不存在");
        VisitorRegistrationDto value = new VisitorRegistrationDto(old.getId(), old.getStudentUserId(), old.getRoomId(),
                old.getBuildingCode(), old.getRoomNo(), old.getVisitorName(), old.getVisitorIdCardMasked(),
                old.getVisitorPhone(), old.getVisitReason(), old.getStartAt(), old.getEndAt(), old.getSubmittedAt(),
                status, auditorId, auditedAt, remark);
        state.visitors.put(Long.valueOf(id), value);
        return value;
    }
}
