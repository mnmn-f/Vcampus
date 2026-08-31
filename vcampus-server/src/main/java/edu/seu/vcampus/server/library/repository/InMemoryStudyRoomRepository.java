package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 无磁盘自习室仓储。 */
public final class InMemoryStudyRoomRepository implements StudyRoomRepository {
    private final Map<Long, StudyRoomView> rooms = new LinkedHashMap<Long, StudyRoomView>();
    private long nextId = 1L;

    public synchronized void seed(StudyRoomView room) {
        if (room == null || room.getId() <= 0) throw new IllegalArgumentException("room");
        rooms.put(room.getId(), room);
        nextId = Math.max(nextId, room.getId() + 1L);
    }

    @Override
    public synchronized PageResult<StudyRoomView> search(Connection c, StudyRoomSearchRequest r) {
        List<StudyRoomView> found = new ArrayList<StudyRoomView>();
        String key = InMemoryLibrarySupport.lower(r.getKeyword());
        for (StudyRoomView room : rooms.values()) {
            String text = InMemoryLibrarySupport.lower(room.getBuildingName() + " " + room.getRoomNo());
            if (key != null && !text.contains(key)) continue;
            if (r.getStatus() != null && !r.getStatus().trim().isEmpty()
                    && !r.getStatus().trim().equalsIgnoreCase(room.getStatus())) continue;
            if (r.getMinCapacity() != null && room.getCapacity() < r.getMinCapacity()) continue;
            found.add(room);
        }
        return InMemoryLibrarySupport.page(found, r.getPage(), r.getPageSize());
    }

    @Override
    public synchronized StudyRoomView findByIdForUpdate(Connection c, long id) {
        return rooms.get(id);
    }

    @Override
    public synchronized StudyRoomView save(Connection c, StudyRoomUpsertRequest r) {
        long id = r.getId() <= 0 ? nextId++ : r.getId();
        StudyRoomView value = new StudyRoomView(id, r.getBuildingName(), r.getRoomNo(),
                r.getCapacity(), r.getOpenTime(), r.getCloseTime(), r.getStatus(), r.getDescription());
        rooms.put(id, value);
        return value;
    }

}
