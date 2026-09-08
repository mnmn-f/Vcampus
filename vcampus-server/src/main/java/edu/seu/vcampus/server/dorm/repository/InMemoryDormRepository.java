package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;

/** 测试和离线演示用内存宿舍仓储；具体读写按职责委托。 */
public final class InMemoryDormRepository extends DormRepositoryComposite {
    private final InMemoryDormState state;

    public InMemoryDormRepository() { this(new InMemoryDormState()); }

    private InMemoryDormRepository(InMemoryDormState value) {
        super(new InMemoryDormFacilityRepository(value), new InMemoryDormSpaceRepository(value),
                new InMemoryDormLeaveRepository(value), new InMemoryDormAccommodationRepository(value),
                new InMemoryDormGovernanceRepository(value), new InMemoryDormBillingRepository(value),
                new InMemoryDormAnnouncementRepository(value));
        state = value;
    }

    public synchronized void addActiveStudent(long userId) { state.activeStudents.add(userId); }
    public synchronized void addBuilding(DormBuildingDto value) {
        state.buildings.put(value.getId(), value);
        state.nextBuilding = Math.max(state.nextBuilding, value.getId() + 1L);
    }
    public synchronized void addRoom(DormRoomDto value) {
        state.rooms.put(value.getId(), value);
        state.nextRoom = Math.max(state.nextRoom, value.getId() + 1L);
    }
    public synchronized void addBed(DormBedDto value) {
        state.beds.put(value.getId(), value);
        state.nextBed = Math.max(state.nextBed, value.getId() + 1L);
    }
    public synchronized void addAccommodation(AccommodationDto value) {
        state.accommodations.put(value.getId(), value);
        state.nextAccommodation = Math.max(state.nextAccommodation, value.getId() + 1L);
        state.activeStudents.add(value.getStudentUserId());
        DormBedDto bed = state.beds.get(value.getBedId());
        if (bed != null && "ACTIVE".equals(value.getStatus())) {
            state.beds.put(value.getBedId(), new DormBedDto(bed.getId(), bed.getRoomId(), bed.getBuildingCode(),
                    bed.getRoomNo(), bed.getBedNo(), "OCCUPIED", Long.valueOf(value.getStudentUserId())));
        }
    }
    public synchronized void addRequest(AccommodationRequestDto value) {
        state.requests.put(value.getId(), value);
        state.nextRequest = Math.max(state.nextRequest, value.getId() + 1L);
        state.activeStudents.add(value.getStudentUserId());
    }
    public synchronized void addAccess(AccessRecordDto value) {
        state.access.put(value.getId(), value);
        state.nextAccess = Math.max(state.nextAccess, value.getId() + 1L);
    }
    public synchronized void addAlert(LateReturnAlertDto value) {
        state.alerts.put(value.getId(), value);
        state.nextAlert = Math.max(state.nextAlert, value.getId() + 1L);
    }
    public synchronized void addInspection(HygieneInspectionDto value) {
        state.hygiene.put(value.getId(), value);
        state.nextHygiene = Math.max(state.nextHygiene, value.getId() + 1L);
    }
    public synchronized void addRepair(RepairOrderDto value) {
        state.repairs.put(value.getId(), value);
        state.nextRepair = Math.max(state.nextRepair, value.getId() + 1L);
    }
    public synchronized void addLeave(edu.seu.vcampus.common.dto.dorm.LeaveRequestDto value) {
        state.leaves.put(value.getId(), value);
        state.nextLeave = Math.max(state.nextLeave, value.getId() + 1L);
        state.activeStudents.add(value.getStudentUserId());
    }
    public synchronized void addBill(long studentId, UtilityBillDto value) {
        state.bills.put(value.getAllocationId(), value);
        state.billOwners.put(value.getAllocationId(), Long.valueOf(studentId));
    }
    public synchronized void addBill(UtilityBillDto value) {
        state.bills.put(value.getAllocationId(), value);
        if (value.getStudentUserId() != null) {
            state.billOwners.put(value.getAllocationId(), value.getStudentUserId());
        }
    }
    public synchronized void addAnnouncement(DormAnnouncementDto value) {
        state.announcements.put(value.getId(), value);
        state.nextAnnouncement = Math.max(state.nextAnnouncement, value.getId() + 1L);
    }
}
