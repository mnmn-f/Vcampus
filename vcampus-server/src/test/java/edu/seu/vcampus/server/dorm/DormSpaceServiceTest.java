package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;
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
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** 宿管空间维护的唯一性、枚举、容量及占用保护测试。 */
public final class DormSpaceServiceTest {
    private InMemoryDormRepository repository;
    private DormService service;
    private SessionContext manager;
    private SessionContext student;

    @Before
    public void setUp() {
        repository = new InMemoryDormRepository();
        repository.addBuilding(new DormBuildingDto(1L, "D1", "一号楼", "校内", "MIXED", "OPEN"));
        repository.addRoom(new DormRoomDto(10L, 1L, "D1", "一零一", "101", 1, 2,
                "STANDARD", "AVAILABLE", null, 0));
        repository.addBed(new DormBedDto(100L, 10L, "D1", "101", "A", "AVAILABLE", null));
        repository.addBed(new DormBedDto(101L, 10L, "D1", "101", "B", "AVAILABLE", null));
        repository.addAccommodation(new AccommodationDto(500L, 11L, 100L, 10L, 1L,
                "D1", "一号楼", "101", "A", LocalDate.now(), null, "ACTIVE"));
        service = new DormService(repository);
        manager = session(90L, Role.DORM_MANAGER);
        student = session(11L, Role.STUDENT);
    }

    @Test
    public void managerCreatesAndUpdatesSpaceWithUniqueCodes() {
        DormBuildingDto building = service.createBuilding(manager,
                new DormBuildingWriteRequest("D2", "二号楼", "校内", "FEMALE", "OPEN"));
        DormRoomDto room = service.createRoom(manager, new DormRoomWriteRequest(building.getId(),
                "201", 2, 4, "SUITE", "AVAILABLE", "新房间"));
        DormBedDto bed = service.createBed(manager, new DormBedWriteRequest(room.getId(), "A", "MAINTENANCE"));
        assertEquals("D2", building.getBuildingCode());
        assertEquals(4, room.getCapacity());
        assertEquals("MAINTENANCE", bed.getStatus());
        try {
            service.createBuilding(manager, new DormBuildingWriteRequest("D2", "重复", "校内", "MIXED", "OPEN"));
            fail("building code must be unique");
        } catch (DormException ex) {
            assertEquals(DormCommands.SPACE_DUPLICATE, ex.getResultCode());
        }
    }

    @Test
    public void activeBedCannotBeMadeAvailableOrRoomCapacityTooSmall() {
        try {
            service.updateBed(manager, new DormBedWriteRequest(100L, 10L, "A", "AVAILABLE"));
            fail("occupied bed must be protected");
        } catch (DormException ex) {
            assertEquals(DormCommands.SPACE_OCCUPIED, ex.getResultCode());
        }
        try {
            service.updateRoom(manager, new DormRoomWriteRequest(10L, 1L, "101", 1,
                    0, "STANDARD", "AVAILABLE", null));
            fail("capacity must be positive");
        } catch (DormException ex) {
            assertEquals(DormCommands.INVALID_INPUT, ex.getResultCode());
        }
    }

    @Test
    public void permissionsAndOccupancyStateAreEnforced() {
        try {
            service.createBed(student, new DormBedWriteRequest(10L, "C", "AVAILABLE"));
            fail("student must not maintain space");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        try {
            service.createBed(manager, new DormBedWriteRequest(10L, "C", "OCCUPIED"));
            fail("new bed cannot be faked as occupied");
        } catch (DormException ex) {
            assertEquals(DormCommands.SPACE_OCCUPIED, ex.getResultCode());
        }
        assertEquals("OCCUPIED", service.updateBed(manager,
                new DormBedWriteRequest(100L, 10L, "A", "OCCUPIED")).getStatus());
    }

    private static SessionContext session(long id, Role role) {
        return new SessionContext("space-" + id + role.name(), id, "u" + id, "用户" + id,
                Collections.unmodifiableSet(EnumSet.of(role)), role);
    }
}
