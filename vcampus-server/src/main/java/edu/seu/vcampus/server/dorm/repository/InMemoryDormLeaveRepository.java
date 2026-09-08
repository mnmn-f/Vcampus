package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 内存请假仓储；所有状态修改和重叠检查在共享状态锁内完成。 */
final class InMemoryDormLeaveRepository implements DormLeaveRepository {
    private final InMemoryDormState state;

    InMemoryDormLeaveRepository(InMemoryDormState state) { this.state = state; }

    @Override public synchronized void lockStudent(Connection c, long studentUserId) { }

    @Override public synchronized boolean hasOverlap(Connection c, long studentUserId,
                                                     LocalDateTime start, LocalDateTime end,
                                                     Long excludedId) {
        for (LeaveRequestDto value : state.leaves.values()) {
            if (value.getStudentUserId() != studentUserId || !active(value.getStatus())) continue;
            if (excludedId != null && value.getId() == excludedId.longValue()) continue;
            if (value.getStartAt().isBefore(end) && start.isBefore(value.getEndAt())) return true;
        }
        return false;
    }

    @Override public synchronized LeaveRequestDto submit(Connection c, long student,
                                                         LeaveSubmitRequest request) {
        long id = state.nextLeave++;
        LeaveRequestDto value = new LeaveRequestDto(id, student, request.getLeaveType(),
                request.getStartAt(), request.getEndAt(), request.getReason(), "PENDING",
                null, null, null, LocalDateTime.now());
        state.leaves.put(id, value);
        state.activeStudents.add(student);
        return value;
    }

    @Override public synchronized DormPage<LeaveRequestDto> list(Connection c, Long student,
                                                                  LeaveQuery query) {
        LeaveQuery q = query == null ? new LeaveQuery() : query;
        List<LeaveRequestDto> rows = new ArrayList<LeaveRequestDto>();
        for (LeaveRequestDto value : state.leaves.values()) {
            if (student != null && value.getStudentUserId() != student.longValue()) continue;
            if (student == null && q.getStudentUserId() != null
                    && value.getStudentUserId() != q.getStudentUserId().longValue()) continue;
            if (q.getStatus() != null && !q.getStatus().equalsIgnoreCase(value.getStatus())) continue;
            if (!dateMatches(value, q)) continue;
            rows.add(value);
        }
        return InMemoryDormSupport.page(rows, q.getPage(), q.getPageSize());
    }

    @Override public synchronized LeaveRequestDto lock(Connection c, long id) {
        return state.leaves.get(id);
    }

    @Override public synchronized LeaveRequestDto cancel(Connection c, long id) {
        LeaveRequestDto old = required(id);
        if (!"PENDING".equals(old.getStatus())) throw invalidState();
        LeaveRequestDto value = copy(old, "CANCELLED", null, null, null);
        state.leaves.put(id, value);
        return value;
    }

    @Override public synchronized LeaveRequestDto review(Connection c, long id, long reviewer,
                                                          boolean approved, String remark) {
        LeaveRequestDto old = required(id);
        if (!"PENDING".equals(old.getStatus())) throw invalidState();
        LeaveRequestDto value = copy(old, approved ? "APPROVED" : "REJECTED", reviewer,
                LocalDateTime.now(), remark);
        state.leaves.put(id, value);
        return value;
    }

    private LeaveRequestDto required(long id) {
        LeaveRequestDto value = state.leaves.get(id);
        if (value == null) throw new DormRepositoryException(DormCommands.LEAVE_NOT_FOUND, "请假申请不存在");
        return value;
    }

    private static LeaveRequestDto copy(LeaveRequestDto old, String status, Long reviewer,
                                        LocalDateTime reviewedAt, String remark) {
        return new LeaveRequestDto(old.getId(), old.getStudentUserId(), old.getLeaveType(),
                old.getStartAt(), old.getEndAt(), old.getReason(), status, reviewer,
                reviewedAt, remark, old.getCreatedAt());
    }

    private static boolean active(String status) {
        return "PENDING".equals(status) || "APPROVED".equals(status);
    }

    private static boolean dateMatches(LeaveRequestDto value, LeaveQuery q) {
        LocalDateTime start = q.getStartDate() == null ? null : q.getStartDate().atStartOfDay();
        LocalDateTime end = q.getEndDate() == null ? null : q.getEndDate().plusDays(1L).atStartOfDay();
        return (start == null || value.getEndAt().isAfter(start))
                && (end == null || value.getStartAt().isBefore(end));
    }

    private static DormRepositoryException invalidState() {
        return new DormRepositoryException(DormCommands.LEAVE_INVALID_STATE, "请假申请当前状态不可操作");
    }
}
