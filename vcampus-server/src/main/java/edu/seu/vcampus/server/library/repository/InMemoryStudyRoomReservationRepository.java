package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 无磁盘预约仓储，用于验证重复和时间重叠规则。 */
public final class InMemoryStudyRoomReservationRepository
        implements StudyRoomReservationRepository {
    private final Map<Long, StudyRoomReservationView> reservations =
            new LinkedHashMap<Long, StudyRoomReservationView>();
    private long nextId = 1L;

    @Override
    public synchronized PageResult<StudyRoomReservationView> search(
            Connection c, StudyRoomReservationSearchRequest r, Long userId) {
        List<StudyRoomReservationView> found = new ArrayList<StudyRoomReservationView>();
        for (StudyRoomReservationView row : reservations.values()) {
            if (userId != null && row.getUserId() != userId.longValue()) continue;
            if (!InMemoryLibrarySupport.same(r.getStatus(), row.getStatus())) continue;
            found.add(row);
        }
        return InMemoryLibrarySupport.page(found, r.getPage(), r.getPageSize());
    }

    @Override
    public synchronized StudyRoomReservationView findById(Connection c, long id) {
        return reservations.get(id);
    }

    @Override
    public synchronized StudyRoomReservationView findByIdForUpdate(Connection c, long id) {
        return findById(c, id);
    }

    @Override
    public synchronized boolean hasRoomOverlap(Connection c, long roomId,
                                               LocalDateTime start, LocalDateTime end) {
        for (StudyRoomReservationView row : reservations.values()) {
            if (row.getRoomId() == roomId && overlaps(row, start, end)) return true;
        }
        return false;
    }

    @Override
    public synchronized boolean hasUserOverlap(Connection c, long userId,
                                               LocalDateTime start, LocalDateTime end) {
        for (StudyRoomReservationView row : reservations.values()) {
            if (row.getUserId() == userId && overlaps(row, start, end)) return true;
        }
        return false;
    }

    private static boolean overlaps(StudyRoomReservationView row, LocalDateTime start,
                                    LocalDateTime end) {
        return "RESERVED".equals(row.getStatus()) && row.getStartAt().isBefore(end)
                && row.getEndAt().isAfter(start);
    }

    @Override
    public synchronized StudyRoomReservationView insert(Connection c, long roomId, long userId,
                                                        LocalDateTime start, LocalDateTime end) {
        StudyRoomReservationView value = new StudyRoomReservationView(nextId++, roomId, null,
                userId, start, end, "RESERVED", null);
        reservations.put(value.getId(), value);
        return value;
    }

    @Override
    public synchronized boolean cancel(Connection c, long id, LocalDateTime cancelledAt) {
        StudyRoomReservationView old = reservations.get(id);
        if (old == null || !"RESERVED".equals(old.getStatus())) return false;
        reservations.put(id, new StudyRoomReservationView(old.getId(), old.getRoomId(),
                old.getRoomName(), old.getUserId(), old.getStartAt(), old.getEndAt(),
                "CANCELLED", cancelledAt));
        return true;
    }

}
