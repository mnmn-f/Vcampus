package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkOrderDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkRequest;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** 维修员接单到完工的流转，以及不该越界的地方。 */
public final class DormRepairWorkflowTest {
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext worker;
    private SessionContext otherWorker;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository();
        service = new DormExtService(repository);
        worker = DormExtTestSupport.session(70L, Role.REPAIR_WORKER);
        otherWorker = DormExtTestSupport.session(71L, Role.REPAIR_WORKER);
        repository.addRoom(1L, "D1", "101");
        repository.setPhone(11L, "13900000001");
        repository.addRepairOrder(500L, 11L, 1L, "PLUMBING", "SUBMITTED");
        repository.setRepairPriority(500L, "URGENT");
    }

    @Test public void queueShowsUnclaimedOrdersWithoutContactPhone() {
        DormPage<RepairWorkOrderDto> queue = service.repairQueue(worker, DormPageQuery.all());
        assertEquals(1L, queue.getTotal());
        RepairWorkOrderDto row = queue.getItems().get(0);
        assertEquals(500L, row.getOrderId());
        assertEquals(RepairWorkOrderDto.SUBMITTED, row.getStatus());
        // 还没接手就先拿到学生电话，没有正当理由。
        assertNull(row.getContactPhone());
    }

    @Test public void acceptThenStartThenReportDoneThenManagerReviewClosesTheOrder() {
        RepairWorkOrderDto accepted = service.acceptRepair(worker, new RepairWorkRequest(500L, null));
        assertEquals(RepairWorkOrderDto.ACCEPTED, accepted.getStatus());
        // 接单之后才给联系方式。
        assertEquals("13900000001", accepted.getContactPhone());
        assertEquals(0L, service.repairQueue(worker, DormPageQuery.all()).getTotal());
        assertEquals(1L, service.repairAssigned(worker, DormPageQuery.all()).getTotal());

        assertEquals(RepairWorkOrderDto.IN_PROGRESS,
                service.startRepair(worker, new RepairWorkRequest(500L, null)).getStatus());
        assertEquals(1L, service.repairAssigned(worker, DormPageQuery.all()).getTotal());
        assertEquals(0L, service.repairHistory(worker, DormPageQuery.all()).getTotal());

        // 维修员报完工只是走到「待宿管审核」，这单从他手上离开但还没结束。
        assertEquals(RepairWorkOrderDto.PENDING_REVIEW,
                service.finishRepair(worker, new RepairWorkRequest(500L, null)).getStatus());
        assertEquals(0L, service.repairAssigned(worker, DormPageQuery.all()).getTotal());
        assertEquals(1L, service.repairHistory(worker, DormPageQuery.all()).getTotal());

        assertEquals(RepairWorkOrderDto.COMPLETED,
                service.reviewRepair(manager(), new RepairWorkRequest(500L, null), true).getStatus());
    }

    @Test public void managerCanSendTheJobBackToTheSameWorker() {
        service.acceptRepair(worker, new RepairWorkRequest(500L, null));
        service.finishRepair(worker, new RepairWorkRequest(500L, null));
        assertEquals(RepairWorkOrderDto.IN_PROGRESS,
                service.reviewRepair(manager(), new RepairWorkRequest(500L, "REJECT"), false).getStatus());
        // 打回之后这单重新回到原来那位维修员的在手列表里。
        assertEquals(1L, service.repairAssigned(worker, DormPageQuery.all()).getTotal());
    }

    @Test public void reviewingAnOrderThatIsNotAwaitingReviewIsRejected() {
        service.acceptRepair(worker, new RepairWorkRequest(500L, null));
        DormExtTestSupport.assertCode(DormExtCommands.REPAIR_INVALID_STATE, new DormExtTestSupport.Action() {
            @Override public void run() { service.reviewRepair(manager(), new RepairWorkRequest(500L, null), true); }
        });
    }

    @Test public void workersCannotReviewTheirOwnWork() {
        service.acceptRepair(worker, new RepairWorkRequest(500L, null));
        service.finishRepair(worker, new RepairWorkRequest(500L, null));
        DormExtTestSupport.assertCode(edu.seu.vcampus.common.protocol.ResultCodes.FORBIDDEN,
                new DormExtTestSupport.Action() {
                    @Override public void run() { service.reviewRepair(worker, new RepairWorkRequest(500L, null), true); }
                });
    }

    private SessionContext manager() {
        return DormExtTestSupport.session(7L, Role.DORM_MANAGER);
    }

    @Test public void secondWorkerCannotStealAnAcceptedOrder() {
        service.acceptRepair(worker, new RepairWorkRequest(500L, null));
        DormExtTestSupport.assertCode(DormExtCommands.REPAIR_ALREADY_TAKEN, new DormExtTestSupport.Action() {
            @Override public void run() { service.acceptRepair(otherWorker, new RepairWorkRequest(500L, null)); }
        });
        DormExtTestSupport.assertCode(DormExtCommands.REPAIR_ALREADY_TAKEN, new DormExtTestSupport.Action() {
            @Override public void run() { service.finishRepair(otherWorker, new RepairWorkRequest(500L, null)); }
        });
    }

    @Test public void startingBeforeAcceptingIsRejected() {
        DormExtTestSupport.assertCode(DormExtCommands.REPAIR_ALREADY_TAKEN, new DormExtTestSupport.Action() {
            @Override public void run() { service.startRepair(worker, new RepairWorkRequest(500L, null)); }
        });
    }

    @Test public void finishingTwiceIsRejectedWithAStateError() {
        service.acceptRepair(worker, new RepairWorkRequest(500L, null));
        service.finishRepair(worker, new RepairWorkRequest(500L, null));
        // 已经报过完工，这单在等宿管审核，维修员不能再报一次。
        DormExtTestSupport.assertCode(DormExtCommands.REPAIR_INVALID_STATE, new DormExtTestSupport.Action() {
            @Override public void run() { service.finishRepair(worker, new RepairWorkRequest(500L, null)); }
        });
    }

    @Test public void missingOrderReportsNotFoundRatherThanAlreadyTaken() {
        DormExtTestSupport.assertCode(DormExtCommands.REPAIR_NOT_FOUND, new DormExtTestSupport.Action() {
            @Override public void run() { service.acceptRepair(worker, new RepairWorkRequest(999L, null)); }
        });
    }

    @Test public void acceptedOrderLeavesTheQueueAndOtherOrdersStayThere() {
        repository.addRepairOrder(501L, 11L, 1L, "LIGHTING", "SUBMITTED");
        service.acceptRepair(worker, new RepairWorkRequest(500L, null));
        DormPage<RepairWorkOrderDto> queue = service.repairQueue(worker, DormPageQuery.all());
        assertEquals(1L, queue.getTotal());
        assertEquals(501L, queue.getItems().get(0).getOrderId());
        // 队列里的单还没派人，不该带出联系电话。
        assertNull(queue.getItems().get(0).getContactPhone());
        DormPage<RepairWorkOrderDto> mine = service.repairAssigned(worker, DormPageQuery.all());
        assertEquals(1L, mine.getTotal());
        assertEquals(500L, mine.getItems().get(0).getOrderId());
        assertNotNull(mine.getItems().get(0).getContactPhone());
        // 另一个维修员看不到别人接走的单。
        assertEquals(0L, service.repairAssigned(otherWorker, DormPageQuery.all()).getTotal());
    }

    @Test public void studentsAndManagersCannotUseTheWorkerQueue() {
        final SessionContext student = DormExtTestSupport.session(11L, Role.STUDENT);
        final SessionContext manager = DormExtTestSupport.session(7L, Role.DORM_MANAGER);
        DormExtTestSupport.assertCode(edu.seu.vcampus.common.protocol.ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            @Override public void run() { service.repairQueue(student, DormPageQuery.all()); }
        });
        DormExtTestSupport.assertCode(edu.seu.vcampus.common.protocol.ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            @Override public void run() { service.repairQueue(manager, DormPageQuery.all()); }
        });
    }
}
