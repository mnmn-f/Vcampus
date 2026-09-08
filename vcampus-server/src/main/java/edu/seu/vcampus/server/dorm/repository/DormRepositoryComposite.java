package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionRequest;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnHandleRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;

import java.sql.Connection;
import java.sql.SQLException;
import org.threeten.bp.LocalDate;

/** 组合各专责仓储，避免 MySQL 与内存实现复制一套转发方法。 */
public abstract class DormRepositoryComposite implements DormRepository {
    private final DormFacilityRepository facilities;
    private final DormSpaceRepository spaces;
    private final DormLeaveRepository leaves;
    private final DormAccommodationRepository accommodation;
    private final DormGovernanceRepository governance;
    private final DormBillingRepository billing;
    private final DormAnnouncementRepository announcements;

    protected DormRepositoryComposite(DormFacilityRepository facilities, DormSpaceRepository spaces,
                                      DormLeaveRepository leaves, DormAccommodationRepository accommodation,
                                      DormGovernanceRepository governance, DormBillingRepository billing,
                                      DormAnnouncementRepository announcements) {
        this.facilities = facilities; this.spaces = spaces; this.leaves = leaves;
        this.accommodation = accommodation; this.governance = governance;
        this.billing = billing; this.announcements = announcements;
    }
    @Override public DormPage<DormBuildingDto> listBuildings(Connection c, DormPageQuery q) throws SQLException { return facilities.listBuildings(c, q); }
    @Override public DormPage<DormRoomDto> listRooms(Connection c, DormPageQuery q) throws SQLException { return facilities.listRooms(c, q); }
    @Override public DormPage<DormBedDto> listBeds(Connection c, DormPageQuery q, boolean occupants) throws SQLException { return facilities.listBeds(c, q, occupants); }
    @Override public DormBedDto findBed(Connection c, long id, boolean lock) throws SQLException { return facilities.findBed(c, id, lock); }
    @Override public boolean roomExists(Connection c, long id) throws SQLException { return facilities.roomExists(c, id); }
    @Override public DormBuildingDto createBuilding(Connection c, DormBuildingWriteRequest r, long actor) throws SQLException { return spaces.createBuilding(c, r, actor); }
    @Override public DormBuildingDto updateBuilding(Connection c, DormBuildingWriteRequest r, long actor) throws SQLException { return spaces.updateBuilding(c, r, actor); }
    @Override public DormRoomDto createRoom(Connection c, DormRoomWriteRequest r, long actor) throws SQLException { return spaces.createRoom(c, r, actor); }
    @Override public DormRoomDto updateRoom(Connection c, DormRoomWriteRequest r, long actor) throws SQLException { return spaces.updateRoom(c, r, actor); }
    @Override public DormBedDto createBed(Connection c, DormBedWriteRequest r, long actor) throws SQLException { return spaces.createBed(c, r, actor); }
    @Override public DormBedDto updateBed(Connection c, DormBedWriteRequest r, long actor) throws SQLException { return spaces.updateBed(c, r, actor); }
    @Override public void lockStudent(Connection c, long student) throws SQLException { leaves.lockStudent(c, student); }
    @Override public boolean hasOverlap(Connection c, long student, org.threeten.bp.LocalDateTime start,
                                       org.threeten.bp.LocalDateTime end, Long excluded) throws SQLException {
        return leaves.hasOverlap(c, student, start, end, excluded);
    }
    @Override public LeaveRequestDto submit(Connection c, long student, LeaveSubmitRequest r) throws SQLException { return leaves.submit(c, student, r); }
    @Override public DormPage<LeaveRequestDto> list(Connection c, Long student, LeaveQuery q) throws SQLException { return leaves.list(c, student, q); }
    @Override public LeaveRequestDto lock(Connection c, long id) throws SQLException { return leaves.lock(c, id); }
    @Override public LeaveRequestDto cancel(Connection c, long id) throws SQLException { return leaves.cancel(c, id); }
    @Override public LeaveRequestDto review(Connection c, long id, long reviewer, boolean approved, String remark) throws SQLException {
        return leaves.review(c, id, reviewer, approved, remark);
    }
    @Override public AccommodationDto findCurrent(Connection c, long id, boolean lock) throws SQLException { return accommodation.findCurrent(c, id, lock); }
    @Override public AccommodationDto findById(Connection c, long id, boolean lock) throws SQLException { return accommodation.findById(c, id, lock); }
    @Override public AccommodationDto assign(Connection c, long student, long bed, LocalDate date, long actor) throws SQLException { return accommodation.assign(c, student, bed, date, actor); }
    @Override public AccommodationDto transfer(Connection c, long student, long record, long bed, LocalDate date, long actor) throws SQLException { return accommodation.transfer(c, student, record, bed, date, actor); }
    @Override public AccommodationDto checkout(Connection c, long student, long record, LocalDate date, long actor) throws SQLException { return accommodation.checkout(c, student, record, date, actor); }
    @Override public AccommodationRequestDto submitRequest(Connection c, long student, String type, Long current, Long bed, String reason) throws SQLException { return accommodation.submitRequest(c, student, type, current, bed, reason); }
    @Override public DormPage<AccommodationRequestDto> listRequests(Connection c, Long student, DormPageQuery q) throws SQLException { return accommodation.listRequests(c, student, q); }
    @Override public AccommodationRequestDto lockRequest(Connection c, long id) throws SQLException { return accommodation.lockRequest(c, id); }
    @Override public AccommodationRequestDto finishRequest(Connection c, long id, long reviewer, boolean approved, String remark) throws SQLException { return accommodation.finishRequest(c, id, reviewer, approved, remark); }
    @Override public AccessRecordDto addAccess(Connection c, long student, AccessRecordDto r) throws SQLException { return governance.addAccess(c, student, r); }
    @Override public DormPage<AccessRecordDto> listAccess(Connection c, Long student, DormPageQuery q) throws SQLException { return governance.listAccess(c, student, q); }
    @Override public DormPage<LateReturnAlertDto> listAlerts(Connection c, Long student, DormPageQuery q) throws SQLException { return governance.listAlerts(c, student, q); }
    @Override public LateReturnAlertDto lockAlert(Connection c, long id) throws SQLException { return governance.lockAlert(c, id); }
    @Override public LateReturnAlertDto handleAlert(Connection c, long id, long actor, LateReturnHandleRequest r) throws SQLException { return governance.handleAlert(c, id, actor, r); }
    @Override public DormPage<HygieneInspectionDto> listHygiene(Connection c, DormPageQuery q) throws SQLException { return governance.listHygiene(c, q); }
    @Override public HygieneInspectionDto saveHygiene(Connection c, HygieneInspectionRequest r, long actor) throws SQLException { return governance.saveHygiene(c, r, actor); }
    @Override public DormPage<RepairOrderDto> listRepairs(Connection c, Long reporter, DormPageQuery q) throws SQLException { return governance.listRepairs(c, reporter, q); }
    @Override public RepairOrderDto createRepair(Connection c, RepairCreateRequest r, long reporter) throws SQLException { return governance.createRepair(c, r, reporter); }
    @Override public RepairOrderDto lockRepair(Connection c, long id) throws SQLException { return governance.lockRepair(c, id); }
    @Override public RepairOrderDto updateRepair(Connection c, RepairStatusRequest r, long actor) throws SQLException { return governance.updateRepair(c, r, actor); }
    @Override public RepairOrderDto evaluateRepair(Connection c, RepairEvaluationRequest r, long student) throws SQLException { return governance.evaluateRepair(c, r, student); }
    @Override public DormPage<UtilityBillDto> listBills(Connection c, long student, DormPageQuery q) throws SQLException { return billing.listBills(c, student, q); }
    @Override public DormPage<UtilityBillDto> listAllBills(Connection c, UtilityBillQuery q) throws SQLException { return billing.listAllBills(c, q); }
    @Override public UtilityBillDto lockAllocation(Connection c, long student, long id) throws SQLException { return billing.lockAllocation(c, student, id); }
    @Override public UtilityBillDto pay(Connection c, long student, UtilityPaymentRequest r) throws SQLException { return billing.pay(c, student, r); }
    @Override public DormPage<DormAnnouncementDto> list(Connection c, DormPageQuery q, boolean drafts) throws SQLException { return announcements.list(c, q, drafts); }
    @Override public DormAnnouncementDto save(Connection c, AnnouncementSaveRequest r, long actor) throws SQLException { return announcements.save(c, r, actor); }
}
