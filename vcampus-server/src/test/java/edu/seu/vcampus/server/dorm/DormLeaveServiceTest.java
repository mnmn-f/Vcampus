package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.LeaveCancelRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveReviewRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** 请假时间重叠、状态机、权限和并发提交测试。 */
public final class DormLeaveServiceTest {
    private DormService service;
    private SessionContext student;
    private SessionContext other;
    private SessionContext manager;

    @Before
    public void setUp() {
        InMemoryDormRepository repository = new InMemoryDormRepository();
        service = new DormService(repository);
        student = session(11L, Role.STUDENT);
        other = session(12L, Role.STUDENT);
        manager = session(90L, Role.DORM_MANAGER);
    }

    @Test
    public void studentOwnListIgnoresPayloadOwnerAndManagerCanReview() {
        service.submitLeave(student, request(at(1), at(3)));
        LeaveRequestDto created = service.ownLeaves(student,
                new LeaveQuery(1, 20, null, 12L, null, null)).getItems().get(0);
        assertEquals(11L, created.getStudentUserId());
        assertEquals(1L, service.manageLeaves(manager, new LeaveQuery()).getTotalElements());
        LeaveRequestDto approved = service.reviewLeave(manager,
                new LeaveReviewRequest(created.getId(), true, "同意"));
        assertEquals("APPROVED", approved.getStatus());
    }

    @Test
    public void pendingAndApprovedIntervalsCannotOverlap() {
        service.submitLeave(student, request(at(1), at(3)));
        try {
            service.submitLeave(student, request(at(2), at(4)));
            fail("overlapping pending leave must fail");
        } catch (DormException ex) {
            assertEquals(DormCommands.LEAVE_OVERLAP, ex.getResultCode());
        }
        LeaveRequestDto adjacent = service.submitLeave(student, request(at(3), at(4)));
        assertEquals("PENDING", adjacent.getStatus());
    }

    @Test
    public void onlyPendingOwnerCanCancelAndReviewIsOneShot() {
        LeaveRequestDto first = service.submitLeave(student, request(at(1), at(2)));
        try {
            service.cancelLeave(other, new LeaveCancelRequest(first.getId()));
            fail("other student must not cancel");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        assertEquals("CANCELLED", service.cancelLeave(student,
                new LeaveCancelRequest(first.getId())).getStatus());
        LeaveRequestDto second = service.submitLeave(student, request(at(4), at(5)));
        service.reviewLeave(manager, new LeaveReviewRequest(second.getId(), false, "拒绝"));
        try {
            service.reviewLeave(manager, new LeaveReviewRequest(second.getId(), true, "重复"));
            fail("review must be one shot");
        } catch (DormException ex) {
            assertEquals(DormCommands.LEAVE_INVALID_STATE, ex.getResultCode());
        }
    }

    @Test
    public void studentCannotReviewAndConcurrentOverlapHasOneWinner() throws Exception {
        final LeaveSubmitRequest value = request(at(10), at(12));
        try {
            service.reviewLeave(student, new LeaveReviewRequest(1L, true, "越权"));
            fail("student must not review");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> left = pool.submit(new SubmitCall(value));
            Future<Boolean> right = pool.submit(new SubmitCall(value));
            assertEquals(1, (left.get() ? 1 : 0) + (right.get() ? 1 : 0));
        } finally {
            pool.shutdownNow();
        }
    }

    private final class SubmitCall implements Callable<Boolean> {
        private final LeaveSubmitRequest request;
        private SubmitCall(LeaveSubmitRequest request) { this.request = request; }
        @Override public Boolean call() {
            try { service.submitLeave(student, request); return Boolean.TRUE; }
            catch (DormException ex) { return Boolean.FALSE; }
        }
    }

    private static LeaveSubmitRequest request(LocalDateTime start, LocalDateTime end) {
        return new LeaveSubmitRequest("PERSONAL", start, end, "回家");
    }
    private static LocalDateTime at(int day) {
        return LocalDate.of(2026, 8, day).atStartOfDay();
    }
    private static SessionContext session(long id, Role role) {
        return new SessionContext("leave-" + id + role.name(), id, "u" + id, "用户" + id,
                Collections.unmodifiableSet(EnumSet.of(role)), role);
    }
}
