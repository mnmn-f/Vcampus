package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.dorm.service.DormStayRules;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/** In-memory stay, access-policy and repair-entry persistence. */
final class InMemoryDormAccessStore {
    private final InMemoryDormExtState state;
    InMemoryDormAccessStore(InMemoryDormExtState state) { this.state = state; }

    void addAccess(long id, long studentId, String type, LocalDateTime at, String door) {
        state.accessRecords.add(new AccessRecordExtDto(id, studentId, type, at, door, false));
    }

    void addRepair(long orderId, long reporterId, long roomId, String category, String status) {
        String[] parts = InMemoryDormSupport.location(state.rooms, roomId);
        state.repairReporters.put(Long.valueOf(orderId), Long.valueOf(reporterId));
        state.repairOrders.put(Long.valueOf(orderId), new RepairEntryPermitDto(orderId, roomId, parts[0], parts[1], category,
                status, LocalDateTime.now(), false, null, state.phones.get(Long.valueOf(reporterId))));
    }

    List<StayStatusDto> stayRows(Long studentId) {
        List<StayStatusDto> rows = new ArrayList<StayStatusDto>();
        for (ResidentAbsenceSnapshot item : state.residentSnapshots) {
            if (studentId != null && studentId.longValue() != item.getStudentUserId()) continue;
            String[] parts = InMemoryDormSupport.location(state.rooms, item.getRoomId());
            rows.add(new StayStatusDto(item.getStudentUserId(), item.getRoomId(), parts[0], parts[1], null,
                    item.getLastExitAt(), item.getLastEntryAt()));
        }
        return rows;
    }

    AccessPolicyDto policy() { return state.accessPolicy; }

    AccessPolicyDto savePolicy(AccessPolicyRequest request) {
        state.accessPolicy = new AccessPolicyDto(request.getCurfewTime(), request.getDawnTime(), LocalDateTime.now());
        return state.accessPolicy;
    }

    DormPage<AccessRecordExtDto> access(long studentId, DormPageQuery query, AccessPolicyDto policy) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<AccessRecordExtDto> rows = new ArrayList<AccessRecordExtDto>();
        for (AccessRecordExtDto item : state.accessRecords) {
            if (item.getStudentUserId() != studentId || !InMemoryDormSupport.matches(q.getKeyword(), item.getDoorName(), item.getRecordType())) continue;
            if (q.getStatus() != null && !q.getStatus().trim().isEmpty() && !q.getStatus().trim().equals(item.getRecordType())) continue;
            rows.add(new AccessRecordExtDto(item.getId(), item.getStudentUserId(), item.getRecordType(), item.getOccurredAt(),
                    item.getDoorName(), DormStayRules.isLateReturn(item.getRecordType(), item.getOccurredAt(), policy.getCurfewTime(), policy.getDawnTime())));
        }
        return InMemoryDormSupport.page(rows, q);
    }

    void setPhone(long userId, String phone) { state.phones.put(Long.valueOf(userId), phone); }
    String reporterPhone(long orderId) {
        Long reporter = state.repairReporters.get(Long.valueOf(orderId));
        return reporter == null ? null : state.phones.get(reporter);
    }
    Long reporter(long orderId) { return state.repairReporters.get(Long.valueOf(orderId)); }

    RepairEntryPermitDto savePermit(RepairEntryPermitRequest request) {
        Long key = Long.valueOf(request.getRepairOrderId());
        RepairEntryPermitDto old = state.repairOrders.get(key);
        if (old == null) throw new DormRepositoryException(DormExtCommands.REPAIR_NOT_FOUND, "报修单不存在");
        Long reporter = state.repairReporters.get(key);
        RepairEntryPermitDto value = new RepairEntryPermitDto(old.getRepairOrderId(), old.getRoomId(), old.getBuildingCode(), old.getRoomNo(),
                old.getCategory(), old.getOrderStatus(), old.getSubmittedAt(), request.isAllowEnter(), request.getNote(),
                reporter == null ? null : state.phones.get(reporter));
        state.repairOrders.put(key, value);
        return value;
    }

    DormPage<RepairEntryPermitDto> permits(long studentId, DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<RepairEntryPermitDto> rows = new ArrayList<RepairEntryPermitDto>();
        for (Map.Entry<Long, RepairEntryPermitDto> entry : state.repairOrders.entrySet()) {
            Long reporter = state.repairReporters.get(entry.getKey());
            if (reporter != null && reporter.longValue() == studentId && InMemoryDormSupport.status(q.getStatus(), entry.getValue().getOrderStatus())) rows.add(entry.getValue());
        }
        return InMemoryDormSupport.page(rows, q);
    }

    int activeResidents(long roomId) {
        int count = 0;
        for (ResidentAbsenceSnapshot item : state.residentSnapshots) if (item.getRoomId() == roomId) count++;
        return count;
    }
}
