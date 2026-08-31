package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.AccessRecordRequest;
import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequest;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormApprovalRequest;
import edu.seu.vcampus.common.dto.dorm.DormAssignmentRequest;
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
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveCancelRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveReviewRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepository;
import edu.seu.vcampus.server.security.SessionContext;


/** 宿舍纵向门面；子服务按住宿、治理、账单和公告职责拆分。 */
public final class DormService {
    private final DormFacilityService facilities;
    private final DormSpaceService spaces;
    private final DormLeaveService leaves;
    private final DormAccommodationService accommodation;
    private final DormGovernanceService governance;
    private final DormBillingService billing;
    private final DormAnnouncementService announcements;

    public DormService(DormRepository repository, TransactionManager transactions) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        facilities = new DormFacilityService(repository, transactions);
        spaces = new DormSpaceService(repository, transactions);
        leaves = new DormLeaveService(repository, transactions);
        accommodation = new DormAccommodationService(repository, transactions);
        governance = new DormGovernanceService(repository, transactions);
        billing = new DormBillingService(repository, transactions);
        announcements = new DormAnnouncementService(repository, transactions);
    }
    public DormService(DormRepository repository) { this(repository, null); }

    public DormPage<DormBuildingDto> buildings(SessionContext s, DormPageQuery q) { return facilities.buildings(s, q); }
    public DormPage<DormRoomDto> rooms(SessionContext s, DormPageQuery q) { return facilities.rooms(s, q); }
    public DormPage<DormBedDto> beds(SessionContext s, DormPageQuery q) { return facilities.beds(s, q); }
    public DormBuildingDto createBuilding(SessionContext s, DormBuildingWriteRequest r) { return spaces.createBuilding(s, r); }
    public DormBuildingDto updateBuilding(SessionContext s, DormBuildingWriteRequest r) { return spaces.updateBuilding(s, r); }
    public DormRoomDto createRoom(SessionContext s, DormRoomWriteRequest r) { return spaces.createRoom(s, r); }
    public DormRoomDto updateRoom(SessionContext s, DormRoomWriteRequest r) { return spaces.updateRoom(s, r); }
    public DormBedDto createBed(SessionContext s, DormBedWriteRequest r) { return spaces.createBed(s, r); }
    public DormBedDto updateBed(SessionContext s, DormBedWriteRequest r) { return spaces.updateBed(s, r); }
    public AccommodationDto mine(SessionContext s) { return accommodation.mine(s); }
    public AccommodationDto assign(SessionContext s, DormAssignmentRequest r) {
        requireAssignment(r); return accommodation.assign(s, r.getStudentUserId(), r.getBedId().longValue(), r.getEffectiveDate());
    }
    public AccommodationDto transfer(SessionContext s, DormAssignmentRequest r) {
        requireAssignment(r); if (r.getRecordId() == null) throw new DormException("DORM.INVALID_INPUT", "住宿记录不能为空");
        return accommodation.transfer(s, r.getStudentUserId(), r.getRecordId().longValue(), r.getBedId().longValue(), r.getEffectiveDate());
    }
    public AccommodationDto checkout(SessionContext s, DormAssignmentRequest r) {
        if (r == null || r.getRecordId() == null) throw new DormException("DORM.INVALID_INPUT", "住宿记录不能为空");
        return accommodation.checkout(s, r.getStudentUserId(), r.getRecordId().longValue(), r.getEffectiveDate());
    }
    public AccommodationRequestDto submitRequest(SessionContext s, AccommodationRequest r) { return accommodation.submit(s, r); }
    public DormPage<AccommodationRequestDto> requests(SessionContext s, DormPageQuery q, Long studentId) { return accommodation.requests(s, q, studentId); }
    public AccommodationRequestDto approveRequest(SessionContext s, DormApprovalRequest r) { return accommodation.approve(s, r); }
    public AccessRecordDto recordAccess(SessionContext s, AccessRecordRequest r) { return governance.recordAccess(s, r); }
    public DormPage<AccessRecordDto> access(SessionContext s, DormPageQuery q, Long studentId) { return governance.access(s, q, studentId); }
    public DormPage<LateReturnAlertDto> alerts(SessionContext s, DormPageQuery q, Long studentId) { return governance.alerts(s, q, studentId); }
    public LateReturnAlertDto handleAlert(SessionContext s, LateReturnHandleRequest r) { return governance.handleAlert(s, r); }
    public DormPage<HygieneInspectionDto> hygiene(SessionContext s, DormPageQuery q) { return governance.hygiene(s, q); }
    public HygieneInspectionDto saveHygiene(SessionContext s, HygieneInspectionRequest r) { return governance.saveHygiene(s, r); }
    public DormPage<RepairOrderDto> repairs(SessionContext s, DormPageQuery q) { return governance.repairs(s, q); }
    public RepairOrderDto createRepair(SessionContext s, RepairCreateRequest r) { return governance.createRepair(s, r); }
    public RepairOrderDto updateRepair(SessionContext s, RepairStatusRequest r) { return governance.updateRepair(s, r); }
    public RepairOrderDto evaluateRepair(SessionContext s, RepairEvaluationRequest r) { return governance.evaluateRepair(s, r); }
    public LeaveRequestDto submitLeave(SessionContext s, LeaveSubmitRequest r) { return leaves.submit(s, r); }
    public DormPage<LeaveRequestDto> ownLeaves(SessionContext s, LeaveQuery q) { return leaves.mine(s, q); }
    public DormPage<LeaveRequestDto> manageLeaves(SessionContext s, LeaveQuery q) { return leaves.manage(s, q); }
    public LeaveRequestDto cancelLeave(SessionContext s, LeaveCancelRequest r) { return leaves.cancel(s, r); }
    public LeaveRequestDto reviewLeave(SessionContext s, LeaveReviewRequest r) { return leaves.review(s, r); }
    public DormPage<UtilityBillDto> bills(SessionContext s, DormPageQuery q) { return billing.mine(s, q); }
    public DormPage<UtilityBillDto> managerBills(SessionContext s, UtilityBillQuery q) { return billing.managerBills(s, q); }
    public UtilityBillDto payBill(SessionContext s, UtilityPaymentRequest r) { return billing.pay(s, r); }
    public DormPage<DormAnnouncementDto> announcements(SessionContext s, DormPageQuery q) { return announcements.list(s, q); }
    public DormAnnouncementDto saveAnnouncement(SessionContext s, AnnouncementSaveRequest r) { return announcements.save(s, r); }

    private static void requireAssignment(DormAssignmentRequest r) {
        if (r == null || r.getBedId() == null) throw new DormException("DORM.INVALID_INPUT", "目标床位不能为空");
    }
}
