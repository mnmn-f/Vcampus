package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.AccommodationRequest;
import edu.seu.vcampus.common.dto.dorm.DormApprovalRequest;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormAssignmentRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormRepository;
import edu.seu.vcampus.server.dorm.registry.DormCommandRegistry;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormService;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import org.threeten.bp.LocalDate;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 宿舍核心闭环：权限、唯一占床、审批状态与重复缴费。 */
public final class DormServiceTest {
    private InMemoryDormRepository repository;
    private DormService service;
    private SessionContext student;
    private SessionContext secondStudent;
    private SessionContext manager;

    @Before
    public void setUp() {
        repository = new InMemoryDormRepository();
        repository.addBuilding(new DormBuildingDto(1, "D1", "一号楼", "校内", "MIXED", "OPEN"));
        repository.addRoom(new DormRoomDto(10, 1, "D1", "一号楼", "101", 1, 2, "STANDARD", "AVAILABLE", null, 0));
        repository.addBed(new DormBedDto(100, 10, "D1", "101", "A", "AVAILABLE", null));
        repository.addBed(new DormBedDto(101, 10, "D1", "101", "B", "AVAILABLE", null));
        repository.addActiveStudent(11);
        repository.addActiveStudent(12);
        repository.addActiveStudent(90);
        service = new DormService(repository);
        student = session(11, Role.STUDENT);
        secondStudent = session(12, Role.STUDENT);
        manager = session(90, Role.DORM_MANAGER);
    }

    @Test
    public void managerAssignLocksBedAndStudentCanReadOwnAccommodation() {
        service.assign(manager, new DormAssignmentRequest(11, 100, LocalDate.now()));
        assertNotNull(service.mine(student));
        try {
            service.assign(manager, new DormAssignmentRequest(12, 100, LocalDate.now()));
        } catch (DormException ex) {
            assertEquals(DormCommands.BED_OCCUPIED, ex.getResultCode());
            return;
        }
        throw new AssertionError("occupied bed must be rejected");
    }

    @Test
    public void requestApprovalIsOneShotAndStudentCannotApprove() {
        long requestId = service.submitRequest(secondStudent,
                new AccommodationRequest("CHECK_IN", null, Long.valueOf(100), "入住"))
                .getRequestId();
        service.approveRequest(manager, new DormApprovalRequest(requestId, true, "通过"));
        try {
            service.approveRequest(manager, new DormApprovalRequest(requestId, true, "重复"));
        } catch (DormException ex) {
            assertEquals(DormCommands.REQUEST_INVALID_STATE, ex.getResultCode());
        }
        try {
            service.approveRequest(student, new DormApprovalRequest(requestId, false, "越权"));
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
            return;
        }
        throw new AssertionError("student must not approve");
    }

    @Test
    public void studentCannotAssignAndRepairMustUseOwnRoom() {
        try {
            service.assign(student, new DormAssignmentRequest(12, 101, LocalDate.now()));
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        try {
            service.createRepair(student, new RepairCreateRequest(10, "LIGHT", "灯坏了", "NORMAL"));
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
            return;
        }
        throw new AssertionError("unaccommodated student must not report a room");
    }

    @Test
    public void paymentIsIdempotencyAndStateProtected() {
        repository.addBill(11, new UtilityBillDto(1, 2, 10, "101", LocalDate.now().minusDays(30),
                LocalDate.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN,
                "UNPAID", "UNPAID", null, null, null));
        service.payBill(student, new UtilityPaymentRequest(1, "pay-1"));
        try {
            service.payBill(student, new UtilityPaymentRequest(1, "pay-2"));
        } catch (DormException ex) {
            assertEquals(DormCommands.BILL_ALREADY_PAID, ex.getResultCode());
            return;
        }
        throw new AssertionError("duplicate payment must be rejected");
    }

    @Test
    public void managerBillRouteIsSeparateFromStudentMineAndPayment() {
        repository.addBill(11, bill(1));
        repository.addBill(12, bill(2));
        SessionManager sessions = new SessionManager();
        SessionContext routeStudent = sessions.createSession(11L, "student", "学生",
                EnumSet.of(Role.STUDENT), Role.STUDENT);
        SessionContext routeManager = sessions.createSession(90L, "manager", "宿管",
                EnumSet.of(Role.DORM_MANAGER), Role.DORM_MANAGER);
        CommandRouter router = DormCommandRegistry.register(new CommandRouter(sessions), service);

        Message studentAll = router.route(Message.request(DormCommands.UTILITY_MANAGER_LIST,
                routeStudent.getSessionToken(), UtilityBillQuery.all()));
        assertEquals(ResultCodes.FORBIDDEN, studentAll.getResultCode());
        Message managerAll = router.route(Message.request(DormCommands.UTILITY_MANAGER_LIST,
                routeManager.getSessionToken(), UtilityBillQuery.all()));
        assertEquals(ResultCodes.OK, managerAll.getResultCode());
        assertEquals(2L, ((DormPage<?>) managerAll.getPayload()).getTotalElements());
        UtilityBillQuery filtered = new UtilityBillQuery(1, 20, "101", "UNPAID", 10L,
                LocalDate.now().minusDays(31), LocalDate.now().plusDays(1));
        Message managerFiltered = router.route(Message.request(DormCommands.UTILITY_MANAGER_LIST,
                routeManager.getSessionToken(), filtered));
        assertEquals(1L, ((DormPage<?>) managerFiltered.getPayload()).getTotalElements());
        Message studentMine = router.route(Message.request(DormCommands.UTILITY_MINE,
                routeStudent.getSessionToken(), DormPageQuery.all()));
        assertEquals(ResultCodes.OK, studentMine.getResultCode());
        assertEquals(1L, ((DormPage<?>) studentMine.getPayload()).getTotalElements());
        Message managerPay = router.route(Message.request(DormCommands.UTILITY_PAY,
                routeManager.getSessionToken(), new UtilityPaymentRequest(1L, "manager-pay")));
        assertEquals(ResultCodes.FORBIDDEN, managerPay.getResultCode());
    }

    @Test
    public void failedTransferLeavesOriginalAccommodationIntact() {
        service.assign(manager, new DormAssignmentRequest(11, 100, LocalDate.now()));
        service.assign(manager, new DormAssignmentRequest(12, 101, LocalDate.now()));
        long recordId = service.mine(student).getId();
        try {
            service.transfer(manager, new DormAssignmentRequest(11, Long.valueOf(recordId), Long.valueOf(101L),
                    LocalDate.now(), "冲突目标"));
        } catch (DormException ex) {
            assertEquals(DormCommands.BED_OCCUPIED, ex.getResultCode());
            assertEquals(100L, service.mine(student).getBedId());
            return;
        }
        throw new AssertionError("transfer to an occupied bed must fail atomically");
    }

    private static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }

    private static UtilityBillDto bill(long allocationId) {
        return new UtilityBillDto(allocationId, allocationId + 10L, 10L, "101",
                LocalDate.now().minusDays(30), LocalDate.now(), BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, "PARTIAL",
                allocationId == 1L ? "UNPAID" : "PAID", null, null, null);
    }
}
