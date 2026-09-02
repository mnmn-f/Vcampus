package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.server.dorm.service.DormHygieneRules;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** In-memory hygiene inspection and task persistence. */
final class InMemoryDormHygieneStore {
    private final InMemoryDormExtState state;
    InMemoryDormHygieneStore(InMemoryDormExtState state) { this.state = state; }

    long createInspection(long roomId, long inspectorId, LocalDateTime inspectedAt, java.math.BigDecimal total,
                          String issue) {
        long id = state.nextInspectionId++;
        String[] parts = InMemoryDormSupport.location(state.rooms, roomId);
        boolean rectify = DormHygieneRules.needRectify(total);
        state.inspections.put(Long.valueOf(id), new HygieneDetailDto(id, roomId, parts[0], parts[1], inspectorId,
                inspectedAt, total, DormHygieneRules.level(total), rectify,
                rectify ? DormHygieneRules.recheckDate(inspectedAt.toLocalDate()) : null, issue, null));
        return id;
    }

    void saveItemScores(long id, List<HygieneItemScoreDto> items) {
        state.itemScores.put(Long.valueOf(id), new ArrayList<HygieneItemScoreDto>(items));
    }

    HygieneDetailDto detail(long id) {
        HygieneDetailDto base = state.inspections.get(Long.valueOf(id));
        if (base == null) return null;
        return new HygieneDetailDto(base.getInspectionId(), base.getRoomId(), base.getBuildingCode(), base.getRoomNo(),
                base.getInspectorId(), base.getInspectedAt(), base.getTotalScore(), base.getScoreLevel(),
                base.isNeedRectify(), base.getRecheckDate(), base.getIssueDescription(), state.itemScores.get(Long.valueOf(id)));
    }

    List<Long> rooms(Long buildingId) { return new ArrayList<Long>(state.rooms.keySet()); }

    void addTask(long id, long roomId, String type, LocalDate date, String status) {
        String[] parts = InMemoryDormSupport.location(state.rooms, roomId);
        state.tasks.put(Long.valueOf(id), new HygieneTaskDto(id, roomId, parts[0], parts[1], type, date, status, null, null));
        if (id >= state.nextTaskId) state.nextTaskId = id + 1;
    }

    boolean createTaskIfAbsent(long roomId, String type, LocalDate date, Long sourceInspectionId) {
        for (HygieneTaskDto item : state.tasks.values()) {
            if (item.getRoomId() == roomId && item.getTaskType().equals(type) && item.getPlanDate().equals(date)) return false;
        }
        long id = state.nextTaskId++;
        String[] parts = InMemoryDormSupport.location(state.rooms, roomId);
        state.tasks.put(Long.valueOf(id), new HygieneTaskDto(id, roomId, parts[0], parts[1], type, date,
                HygieneTaskDto.STATUS_PENDING, null, sourceInspectionId));
        return true;
    }

    int markTasksDone(long roomId, LocalDate onOrBefore, long inspectionId) {
        int done = 0;
        for (Map.Entry<Long, HygieneTaskDto> entry : state.tasks.entrySet()) {
            HygieneTaskDto item = entry.getValue();
            if (item.getRoomId() == roomId && HygieneTaskDto.STATUS_PENDING.equals(item.getStatus())
                    && !item.getPlanDate().isAfter(onOrBefore)) {
                entry.setValue(new HygieneTaskDto(item.getId(), item.getRoomId(), item.getBuildingCode(), item.getRoomNo(),
                        item.getTaskType(), item.getPlanDate(), HygieneTaskDto.STATUS_DONE, Long.valueOf(inspectionId), item.getSourceInspectionId()));
                done++;
            }
        }
        return done;
    }

    DormPage<HygieneTaskDto> listTasks(DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<HygieneTaskDto> rows = new ArrayList<HygieneTaskDto>();
        for (HygieneTaskDto item : state.tasks.values()) {
            boolean room = q.getRoomId() == null || q.getRoomId().longValue() == item.getRoomId();
            if (room && InMemoryDormSupport.status(q.getStatus(), item.getStatus())
                    && InMemoryDormSupport.matches(q.getKeyword(), item.getRoomNo(), item.getBuildingCode(), item.getTaskType())) rows.add(item);
        }
        Collections.sort(rows, new Comparator<HygieneTaskDto>() {
            @Override public int compare(HygieneTaskDto a, HygieneTaskDto b) {
                int result = rank(a.getStatus()) - rank(b.getStatus());
                if (result != 0) return result;
                result = (a.isRecheck() ? 0 : 1) - (b.isRecheck() ? 0 : 1);
                if (result != 0) return result;
                result = a.getPlanDate().compareTo(b.getPlanDate());
                return result != 0 ? result : Long.valueOf(a.getId()).compareTo(Long.valueOf(b.getId()));
            }
            private int rank(String status) {
                if (HygieneTaskDto.STATUS_PENDING.equals(status)) return 0;
                return HygieneTaskDto.STATUS_SKIPPED.equals(status) ? 1 : 2;
            }
        });
        return InMemoryDormSupport.page(rows, q);
    }
}
