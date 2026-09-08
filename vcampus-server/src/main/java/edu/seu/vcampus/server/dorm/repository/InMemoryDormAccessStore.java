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
    void setPriority(long orderId, String priority) { state.repairPriorities.put(Long.valueOf(orderId), priority); }
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

    // ---------- 维修员工作流 ----------

    /**
     * 内存版的报修工单借用 {@link RepairEntryPermitDto} 存放，它没有处理人和状态可改，
     * 所以状态流转在这里表现为「换一份带新状态的副本」，处理人另用一张表记。
     */
    DormPage<RepairWorkOrderDto> repairQueue(DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<RepairWorkOrderDto> rows = new ArrayList<RepairWorkOrderDto>();
        for (Map.Entry<Long, RepairEntryPermitDto> entry : state.repairOrders.entrySet()) {
            if (!"SUBMITTED".equals(entry.getValue().getOrderStatus())) continue;
            if (state.repairHandlers.containsKey(entry.getKey())) continue;
            if (!InMemoryDormSupport.matches(q.getKeyword(), entry.getValue().getRoomNo(), entry.getValue().getCategory())) continue;
            rows.add(workOrder(entry.getKey(), entry.getValue(), false));
        }
        return InMemoryDormSupport.page(rows, q);
    }

    DormPage<RepairWorkOrderDto> repairAssigned(long handlerId, DormPageQuery query, boolean active) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<RepairWorkOrderDto> rows = new ArrayList<RepairWorkOrderDto>();
        for (Map.Entry<Long, RepairEntryPermitDto> entry : state.repairOrders.entrySet()) {
            Long handler = state.repairHandlers.get(entry.getKey());
            if (handler == null || handler.longValue() != handlerId) continue;
            if (activeStatus(entry.getValue().getOrderStatus()) != active) continue;
            if (!InMemoryDormSupport.matches(q.getKeyword(), entry.getValue().getRoomNo(), entry.getValue().getCategory())) continue;
            rows.add(workOrder(entry.getKey(), entry.getValue(), true));
        }
        return InMemoryDormSupport.page(rows, q);
    }

    /** 内存实现没有用户表，维修员名单由测试用 {@code addRepairWorker} 直接喂进来。 */
    List<RepairWorkerDto> repairWorkers() {
        List<RepairWorkerDto> rows = new ArrayList<RepairWorkerDto>();
        for (Map.Entry<Long, String> entry : state.repairWorkers.entrySet()) {
            int active = 0;
            for (Map.Entry<Long, Long> assigned : state.repairHandlers.entrySet()) {
                if (!assigned.getValue().equals(entry.getKey())) continue;
                RepairEntryPermitDto order = state.repairOrders.get(assigned.getKey());
                if (order != null && activeStatus(order.getOrderStatus())) active++;
            }
            rows.add(new RepairWorkerDto(entry.getKey().longValue(), entry.getValue(), active));
        }
        return rows;
    }

    int assignRepair(long orderId, long workerId) {
        Long key = Long.valueOf(orderId);
        RepairEntryPermitDto value = state.repairOrders.get(key);
        if (value == null) return 0;
        String status = value.getOrderStatus();
        if (!"SUBMITTED".equals(status) && !activeStatus(status)) return 0;
        state.repairHandlers.put(key, Long.valueOf(workerId));
        if ("SUBMITTED".equals(status)) state.repairOrders.put(key, withStatus(value, "ACCEPTED"));
        return 1;
    }

    RepairWorkOrderDto findRepairForManager(long orderId) {
        RepairEntryPermitDto value = state.repairOrders.get(Long.valueOf(orderId));
        return value == null ? null : workOrder(Long.valueOf(orderId), value, true);
    }

    void addRepairWorker(long userId, String displayName) {
        state.repairWorkers.put(Long.valueOf(userId), displayName);
    }

    RepairWorkOrderDto findRepairWork(long orderId, long handlerId) {
        RepairEntryPermitDto value = state.repairOrders.get(Long.valueOf(orderId));
        if (value == null) return null;
        Long handler = state.repairHandlers.get(Long.valueOf(orderId));
        return workOrder(Long.valueOf(orderId), value, handler != null && handler.longValue() == handlerId);
    }

    int claimRepair(long orderId, long handlerId) {
        Long key = Long.valueOf(orderId);
        RepairEntryPermitDto value = state.repairOrders.get(key);
        if (value == null || !"SUBMITTED".equals(value.getOrderStatus())) return 0;
        if (state.repairHandlers.containsKey(key)) return 0;
        state.repairHandlers.put(key, Long.valueOf(handlerId));
        state.repairOrders.put(key, withStatus(value, "ACCEPTED"));
        return 1;
    }

    int startRepair(long orderId, long handlerId) {
        return advance(orderId, handlerId, "ACCEPTED", "IN_PROGRESS");
    }

    int finishRepair(long orderId, long handlerId) {
        int done = advance(orderId, handlerId, "ACCEPTED", "PENDING_REVIEW");
        return done == 1 ? done : advance(orderId, handlerId, "IN_PROGRESS", "PENDING_REVIEW");
    }

    int reviewRepair(long orderId, boolean approved) {
        Long key = Long.valueOf(orderId);
        RepairEntryPermitDto value = state.repairOrders.get(key);
        if (value == null || !"PENDING_REVIEW".equals(value.getOrderStatus())) return 0;
        state.repairOrders.put(key, withStatus(value, approved ? "COMPLETED" : "IN_PROGRESS"));
        return 1;
    }

    private int advance(long orderId, long handlerId, String from, String to) {
        Long key = Long.valueOf(orderId);
        RepairEntryPermitDto value = state.repairOrders.get(key);
        Long handler = state.repairHandlers.get(key);
        if (value == null || handler == null || handler.longValue() != handlerId) return 0;
        if (!from.equals(value.getOrderStatus())) return 0;
        state.repairOrders.put(key, withStatus(value, to));
        return 1;
    }

    private static boolean activeStatus(String status) {
        return "ACCEPTED".equals(status) || "IN_PROGRESS".equals(status);
    }

    private static RepairEntryPermitDto withStatus(RepairEntryPermitDto value, String status) {
        return new RepairEntryPermitDto(value.getRepairOrderId(), value.getRoomId(), value.getBuildingCode(),
                value.getRoomNo(), value.getCategory(), status, value.getSubmittedAt(),
                value.isAllowEnter(), value.getNote(), value.getContactPhone());
    }

    private RepairWorkOrderDto workOrder(Long key, RepairEntryPermitDto value, boolean withPhone) {
        Long reporter = state.repairReporters.get(key);
        String priority = state.repairPriorities.get(key);
        return new RepairWorkOrderDto(value.getRepairOrderId(), value.getRoomId(), value.getBuildingCode(),
                value.getRoomNo(), value.getCategory(), null, priority == null ? "NORMAL" : priority,
                value.getOrderStatus(), reporter, null, value.getSubmittedAt(), null, null,
                state.repairHandlers.get(key), value.isAllowEnter(), value.getNote(),
                withPhone ? value.getContactPhone() : null, null);
    }

    int activeResidents(long roomId) {
        int count = 0;
        for (ResidentAbsenceSnapshot item : state.residentSnapshots) if (item.getRoomId() == roomId) count++;
        return count;
    }
}
