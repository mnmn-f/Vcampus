package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RoomDeleteRequest;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.dorm.service.DormStayRules;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 在宿状态判定、晚归策略、报修入内授权与房间删除前置条件。 */
public final class DormStayAndAccessTest {
    private static final LocalDateTime EXIT_AT = LocalDateTime.of(2026, 9, 1, 20, 0);
    private static final LocalDateTime ENTRY_AT = LocalDateTime.of(2026, 9, 1, 22, 0);

    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext student;
    private SessionContext manager;

    @Before
    public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addRoom(10L, "D1", "101");
        repository.addRoom(20L, "D1", "102");
        repository.addResident(11L, 10L, EXIT_AT, ENTRY_AT);
        service = new DormExtService(repository);
        student = session(11L, Role.STUDENT);
        manager = session(90L, Role.DORM_MANAGER);
    }

    // ---------- 在宿状态 ----------

    @Test
    public void backAfterExitCountsAsInDorm() {
        assertEquals(StayStatusDto.IN_DORM,
                DormStayRules.status(EXIT_AT, ENTRY_AT, false));
    }

    @Test
    public void exitWithoutReturnCountsAsOut() {
        assertEquals(StayStatusDto.OUT, DormStayRules.status(EXIT_AT, null, false));
    }

    @Test
    public void approvedLeaveOutranksAccessRecords() {
        assertEquals("请了假的人不在宿舍是登记过的，不该混进擅自离宿",
                StayStatusDto.LEAVE_REGISTERED, DormStayRules.status(EXIT_AT, null, true));
    }

    @Test
    public void noAccessHistoryCountsAsInDorm() {
        assertEquals(StayStatusDto.IN_DORM, DormStayRules.status(null, null, false));
    }

    @Test
    public void studentSeesOwnStayStatus() {
        StayStatusDto value = service.myStayStatus(student);
        assertEquals(StayStatusDto.IN_DORM, value.getStatus());
        assertEquals("D1", value.getBuildingCode());
        assertEquals("101", value.getRoomNo());
    }

    @Test
    public void leaveMakesStatusRegistered() {
        repository.addApprovedLeave(11L, LocalDate.now());
        assertEquals(StayStatusDto.LEAVE_REGISTERED, service.myStayStatus(student).getStatus());
    }

    @Test
    public void studentWithoutAccommodationHasNoStatus() {
        try {
            service.myStayStatus(session(77L, Role.STUDENT));
            fail("没有住宿记录时不应返回在宿状态");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.NO_ACCOMMODATION, ex.getResultCode());
        }
    }

    @Test
    public void managerSeesEveryResident() {
        assertEquals(1L, service.stayStatuses(manager).getTotalElements());
    }

    // ---------- 晚归判定 ----------

    @Test
    public void entryAfterCurfewIsLate() {
        assertTrue(DormStayRules.isLateReturn("ENTRY", LocalDateTime.of(2026, 9, 1, 23, 30),
                LocalTime.of(23, 0), LocalTime.of(5, 0)));
    }

    @Test
    public void entryBeforeDawnIsAlsoLate() {
        assertTrue("凌晨两点回来在时钟上早，在管理上是整夜未归",
                DormStayRules.isLateReturn("ENTRY", LocalDateTime.of(2026, 9, 2, 2, 0),
                        LocalTime.of(23, 0), LocalTime.of(5, 0)));
    }

    @Test
    public void ordinaryEntryIsNotLate() {
        assertFalse(DormStayRules.isLateReturn("ENTRY", LocalDateTime.of(2026, 9, 1, 21, 0),
                LocalTime.of(23, 0), LocalTime.of(5, 0)));
    }

    @Test
    public void exitIsNeverLate() {
        assertFalse("离宿记录没有晚归一说",
                DormStayRules.isLateReturn("EXIT", LocalDateTime.of(2026, 9, 1, 23, 30),
                        LocalTime.of(23, 0), LocalTime.of(5, 0)));
    }

    @Test
    public void accessListMarksLateReturns() {
        repository.addAccessRecord(1L, 11L, "ENTRY", LocalDateTime.of(2026, 9, 1, 21, 0), "南门");
        repository.addAccessRecord(2L, 11L, "ENTRY", LocalDateTime.of(2026, 9, 2, 23, 40), "南门");
        List<AccessRecordExtDto> rows =
                service.myAccessRecords(student, DormPageQuery.all()).getItems();
        assertEquals(2, rows.size());
        assertFalse(rows.get(0).isLateReturn());
        assertTrue(rows.get(1).isLateReturn());
    }

    @Test
    public void accessListFiltersByRecordType() {
        // 学生端「仅入宿／仅离宿」下拉走的是 DormPageQuery.status，
        // 复用同一个字段是为了不给分页查询再加一个参数。
        repository.addAccessRecord(1L, 11L, "ENTRY", LocalDateTime.of(2026, 9, 1, 21, 0), "南门");
        repository.addAccessRecord(2L, 11L, "EXIT", LocalDateTime.of(2026, 9, 2, 7, 0), "南门");
        List<AccessRecordExtDto> entries = service.myAccessRecords(student,
                new DormPageQuery(1, 20, null, "ENTRY", null, null)).getItems();
        assertEquals(1, entries.size());
        assertEquals("ENTRY", entries.get(0).getRecordType());

        List<AccessRecordExtDto> exits = service.myAccessRecords(student,
                new DormPageQuery(1, 20, null, "EXIT", null, null)).getItems();
        assertEquals(1, exits.size());
        assertEquals("EXIT", exits.get(0).getRecordType());

        assertEquals("不选类型时两条都要在", 2, service.myAccessRecords(student,
                DormPageQuery.all()).getItems().size());
    }

    @Test
    public void policyChangeReclassifiesHistory() {
        repository.addAccessRecord(1L, 11L, "ENTRY", LocalDateTime.of(2026, 9, 1, 22, 30), "南门");
        assertFalse(service.myAccessRecords(student, DormPageQuery.all())
                .getItems().get(0).isLateReturn());

        service.saveAccessPolicy(manager,
                new AccessPolicyRequest(LocalTime.of(22, 0), LocalTime.of(5, 0)));
        assertTrue("门禁时间提前后，同一条历史记录应当按新策略重新判定",
                service.myAccessRecords(student, DormPageQuery.all())
                        .getItems().get(0).isLateReturn());
    }

    @Test
    public void dawnMustBeBeforeCurfew() {
        try {
            service.saveAccessPolicy(manager,
                    new AccessPolicyRequest(LocalTime.of(5, 0), LocalTime.of(23, 0)));
            fail("清晨时间晚于门禁时间会把整天判成晚归");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.POLICY_INVALID, ex.getResultCode());
        }
    }

    @Test
    public void defaultPolicyIsReadable() {
        AccessPolicyDto policy = service.accessPolicy(manager);
        assertEquals(LocalTime.of(23, 0), policy.getCurfewTime());
        assertEquals(LocalTime.of(5, 0), policy.getDawnTime());
    }

    // ---------- 报修入内授权 ----------

    @Test
    public void studentAuthorisesOwnRepairOrder() {
        repository.addRepairOrder(500L, 11L, 10L, "WATER", "SUBMITTED");
        RepairEntryPermitDto saved = service.setRepairPermit(student,
                new RepairEntryPermitRequest(500L, true, "钥匙在宿管处"));
        assertTrue(saved.isAllowEnter());
        assertEquals("钥匙在宿管处", saved.getNote());
    }

    @Test
    public void studentCannotAuthoriseOthersRepairOrder() {
        repository.addRepairOrder(500L, 12L, 10L, "WATER", "SUBMITTED");
        try {
            service.setRepairPermit(student, new RepairEntryPermitRequest(500L, true, null));
            fail("不能为别人的报修单授权");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    @Test
    public void authorisingUnknownRepairOrderIsRejected() {
        try {
            service.setRepairPermit(student, new RepairEntryPermitRequest(999L, true, null));
            fail("不存在的报修单应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.REPAIR_NOT_FOUND, ex.getResultCode());
        }
    }

    @Test
    public void permitDefaultsToDenied() {
        repository.addRepairOrder(500L, 11L, 10L, "WATER", "SUBMITTED");
        assertFalse("没有显式授权时默认不允许入内",
                service.myRepairPermits(student, DormPageQuery.all())
                        .getItems().get(0).isAllowEnter());
    }

    @Test
    public void authorisingWithoutAPhoneRequiresAContactNote() {
        // 维修人员要在学生不在场时进门，必须有个能当场联系上的方式。
        repository.addRepairOrder(500L, 11L, 10L, "WATER", "SUBMITTED");
        try {
            service.setRepairPermit(student, new RepairEntryPermitRequest(500L, true, null));
            fail("没手机号又没留备注不应放行");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.CONTACT_REQUIRED, ex.getResultCode());
        }
    }

    @Test
    public void registeredPhoneMakesTheNoteOptional() {
        repository.setPhone(11L, "13800000000");
        repository.addRepairOrder(500L, 11L, 10L, "WATER", "SUBMITTED");
        RepairEntryPermitDto saved = service.setRepairPermit(student,
                new RepairEntryPermitRequest(500L, true, null));
        assertTrue(saved.isAllowEnter());
        assertEquals("联系电话取自账号资料，不另存一份副本", "13800000000",
                saved.getContactPhone());
    }

    @Test
    public void revokingNeverNeedsAContact() {
        // 撤销授权意味着没人会进门，自然不需要留联系方式。
        repository.addRepairOrder(500L, 11L, 10L, "WATER", "SUBMITTED");
        assertFalse(service.setRepairPermit(student,
                new RepairEntryPermitRequest(500L, false, null)).isAllowEnter());
    }

    // ---------- 房间删除 ----------

    @Test
    public void emptyRoomCanBeDeleted() {
        assertEquals(Long.valueOf(20L), service.deleteRoom(manager, new RoomDeleteRequest(20L)));
    }

    @Test
    public void occupiedRoomCannotBeDeleted() {
        try {
            service.deleteRoom(manager, new RoomDeleteRequest(10L));
            fail("有在住学生的房间不应能删除");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.ROOM_OCCUPIED, ex.getResultCode());
        }
    }

    @Test
    public void roomWithHistoryCannotBeDeleted() {
        repository.addRepairOrder(500L, 11L, 20L, "WATER", "SUBMITTED");
        try {
            service.deleteRoom(manager, new RoomDeleteRequest(20L));
            fail("有历史业务数据的房间不应能删除");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.ROOM_HAS_HISTORY, ex.getResultCode());
        }
    }

    @Test
    public void studentCannotDeleteRoom() {
        try {
            service.deleteRoom(student, new RoomDeleteRequest(20L));
            fail("学生没有空间维护权限");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    private static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }
}
