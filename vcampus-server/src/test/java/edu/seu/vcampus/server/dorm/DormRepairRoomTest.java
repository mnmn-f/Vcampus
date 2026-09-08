package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RoomDeleteRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 报修入内授权与房间删除前置条件。 */
public final class DormRepairRoomTest {
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext student;
    private SessionContext manager;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository(); repository.addRoom(10L, "D1", "101"); repository.addRoom(20L, "D1", "102");
        repository.addResident(11L, 10L, LocalDateTime.now(), null);
        service = new DormExtService(repository); student = DormExtTestSupport.session(11L, Role.STUDENT);
        manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
    }
    @Test public void permitDefaultsToDeniedAndStudentCanAuthorizeOwnRepair() {
        repository.addRepairOrder(500L, 11L, 10L, "WATER", "SUBMITTED");
        assertFalse(service.myRepairPermits(student, DormPageQuery.all()).getItems().get(0).isAllowEnter());
        RepairEntryPermitDto saved = service.setRepairPermit(student,
                new RepairEntryPermitRequest(500L, true, "钥匙在宿管处"));
        assertTrue(saved.isAllowEnter()); assertEquals("钥匙在宿管处", saved.getNote());
    }
    @Test public void ownershipAndUnknownRepairAreChecked() {
        repository.addRepairOrder(500L, 12L, 10L, "WATER", "SUBMITTED");
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.setRepairPermit(student, new RepairEntryPermitRequest(500L, true, null)); }
        });
        DormExtTestSupport.assertCode(DormExtCommands.REPAIR_NOT_FOUND, new DormExtTestSupport.Action() {
            public void run() { service.setRepairPermit(student, new RepairEntryPermitRequest(999L, true, null)); }
        });
    }
    @Test public void contactNoteIsRequiredWithoutPhoneButPhoneIsReused() {
        repository.addRepairOrder(500L, 11L, 10L, "WATER", "SUBMITTED");
        DormExtTestSupport.assertCode(DormExtCommands.CONTACT_REQUIRED, new DormExtTestSupport.Action() {
            public void run() { service.setRepairPermit(student, new RepairEntryPermitRequest(500L, true, null)); }
        });
        repository.setPhone(11L, "13800000000");
        RepairEntryPermitDto saved = service.setRepairPermit(student,
                new RepairEntryPermitRequest(500L, true, null));
        assertTrue(saved.isAllowEnter()); assertEquals("13800000000", saved.getContactPhone());
    }
    @Test public void revokeDoesNotNeedContact() {
        repository.addRepairOrder(500L, 11L, 10L, "WATER", "SUBMITTED");
        assertFalse(service.setRepairPermit(student,
                new RepairEntryPermitRequest(500L, false, null)).isAllowEnter());
    }
    @Test public void emptyRoomCanBeDeletedButOccupiedOrHistoricalCannot() {
        assertEquals(Long.valueOf(20L), service.deleteRoom(manager, new RoomDeleteRequest(20L)));
        DormExtTestSupport.assertCode(DormExtCommands.ROOM_OCCUPIED, new DormExtTestSupport.Action() {
            public void run() { service.deleteRoom(manager, new RoomDeleteRequest(10L)); }
        });
    }
    @Test public void roomHistoryAndStudentPermissionBlockDeletion() {
        repository.addRepairOrder(500L, 11L, 20L, "WATER", "SUBMITTED");
        DormExtTestSupport.assertCode(DormExtCommands.ROOM_HAS_HISTORY, new DormExtTestSupport.Action() {
            public void run() { service.deleteRoom(manager, new RoomDeleteRequest(20L)); }
        });
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.deleteRoom(student, new RoomDeleteRequest(20L)); }
        });
    }
}
