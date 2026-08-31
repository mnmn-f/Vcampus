package edu.seu.vcampus.client.service.dorm;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccessRecordRequest;
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
import edu.seu.vcampus.common.dto.dorm.LeaveCancelRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveReviewRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormCommands;

import java.io.Serializable;

/** 真实宿舍网络客户端；页面只依赖 DormClientService。 */
public final class NetworkDormClientService implements DormClientService {
    private final NetworkClientService network;
    private final ClientSession session;

    public NetworkDormClientService(NetworkClientService network, ClientSession session) {
        if (network == null || session == null) throw new IllegalArgumentException("dorm client dependencies required");
        this.network = network;
        this.session = session;
    }
    public NetworkDormClientService(ClientGateway gateway, ClientSession session) {
        this(new NetworkClientService(gateway), session);
    }

    @Override public DormPage<DormBuildingDto> buildings(DormPageQuery q) throws NetworkClientException { return page(call(DormCommands.BUILDING_LIST, q)); }
    @Override public DormPage<DormRoomDto> rooms(DormPageQuery q) throws NetworkClientException { return page(call(DormCommands.ROOM_LIST, q)); }
    @Override public DormPage<DormBedDto> beds(DormPageQuery q) throws NetworkClientException { return page(call(DormCommands.BED_LIST, q)); }
    @Override public DormBuildingDto createBuilding(DormBuildingWriteRequest r) throws NetworkClientException { return value(call(DormCommands.BUILDING_CREATE, r), DormBuildingDto.class); }
    @Override public DormBuildingDto updateBuilding(DormBuildingWriteRequest r) throws NetworkClientException { return value(call(DormCommands.BUILDING_UPDATE, r), DormBuildingDto.class); }
    @Override public DormRoomDto createRoom(DormRoomWriteRequest r) throws NetworkClientException { return value(call(DormCommands.ROOM_CREATE, r), DormRoomDto.class); }
    @Override public DormRoomDto updateRoom(DormRoomWriteRequest r) throws NetworkClientException { return value(call(DormCommands.ROOM_UPDATE, r), DormRoomDto.class); }
    @Override public DormBedDto createBed(DormBedWriteRequest r) throws NetworkClientException { return value(call(DormCommands.BED_CREATE, r), DormBedDto.class); }
    @Override public DormBedDto updateBed(DormBedWriteRequest r) throws NetworkClientException { return value(call(DormCommands.BED_UPDATE, r), DormBedDto.class); }
    @Override public AccommodationDto myAccommodation() throws NetworkClientException { return value(call(DormCommands.ACCOMMODATION_MINE, null), AccommodationDto.class); }
    @Override public AccommodationDto assign(DormAssignmentRequest r) throws NetworkClientException { return value(call(DormCommands.ACCOMMODATION_ASSIGN, r), AccommodationDto.class); }
    @Override public AccommodationDto transfer(DormAssignmentRequest r) throws NetworkClientException { return value(call(DormCommands.ACCOMMODATION_TRANSFER, r), AccommodationDto.class); }
    @Override public AccommodationDto checkout(DormAssignmentRequest r) throws NetworkClientException { return value(call(DormCommands.ACCOMMODATION_CHECKOUT, r), AccommodationDto.class); }
    @Override public AccommodationRequestDto submitRequest(AccommodationRequest r) throws NetworkClientException { return value(call(DormCommands.REQUEST_SUBMIT, r), AccommodationRequestDto.class); }
    @Override public DormPage<AccommodationRequestDto> requests(DormPageQuery q, Long id) throws NetworkClientException { return page(call(DormCommands.REQUEST_LIST, new edu.seu.vcampus.common.dto.dorm.DormQuery(q, id))); }
    @Override public AccommodationRequestDto approveRequest(DormApprovalRequest r) throws NetworkClientException { return value(call(DormCommands.REQUEST_APPROVE, r), AccommodationRequestDto.class); }
    @Override public AccessRecordDto recordAccess(AccessRecordRequest r) throws NetworkClientException { return value(call(DormCommands.ACCESS_RECORD, r), AccessRecordDto.class); }
    @Override public DormPage<AccessRecordDto> access(DormPageQuery q, Long id) throws NetworkClientException { return page(call(DormCommands.ACCESS_LIST, new edu.seu.vcampus.common.dto.dorm.DormQuery(q, id))); }
    @Override public DormPage<LateReturnAlertDto> alerts(DormPageQuery q, Long id) throws NetworkClientException { return page(call(DormCommands.ALERT_LIST, new edu.seu.vcampus.common.dto.dorm.DormQuery(q, id))); }
    @Override public LateReturnAlertDto handleAlert(LateReturnHandleRequest r) throws NetworkClientException { return value(call(DormCommands.ALERT_HANDLE, r), LateReturnAlertDto.class); }
    @Override public DormPage<HygieneInspectionDto> hygiene(DormPageQuery q) throws NetworkClientException { return page(call(DormCommands.HYGIENE_LIST, q)); }
    @Override public HygieneInspectionDto saveHygiene(HygieneInspectionRequest r) throws NetworkClientException { return value(call(DormCommands.HYGIENE_SAVE, r), HygieneInspectionDto.class); }
    @Override public DormPage<RepairOrderDto> repairs(DormPageQuery q) throws NetworkClientException { return page(call(DormCommands.REPAIR_LIST, q)); }
    @Override public RepairOrderDto createRepair(RepairCreateRequest r) throws NetworkClientException { return value(call(DormCommands.REPAIR_CREATE, r), RepairOrderDto.class); }
    @Override public RepairOrderDto updateRepair(RepairStatusRequest r) throws NetworkClientException { return value(call(DormCommands.REPAIR_UPDATE, r), RepairOrderDto.class); }
    @Override public RepairOrderDto evaluateRepair(RepairEvaluationRequest r) throws NetworkClientException { return value(call(DormCommands.REPAIR_EVALUATE, r), RepairOrderDto.class); }
    @Override public LeaveRequestDto submitLeave(LeaveSubmitRequest r) throws NetworkClientException { return value(call(DormCommands.LEAVE_SUBMIT, r), LeaveRequestDto.class); }
    @Override public DormPage<LeaveRequestDto> myLeaves(LeaveQuery q) throws NetworkClientException { return page(call(DormCommands.LEAVE_MINE, myLeaveQuery(q))); }
    @Override public LeaveRequestDto cancelLeave(LeaveCancelRequest r) throws NetworkClientException { return value(call(DormCommands.LEAVE_CANCEL, r), LeaveRequestDto.class); }
    @Override public DormPage<LeaveRequestDto> managerLeaves(LeaveQuery q) throws NetworkClientException { return page(call(DormCommands.LEAVE_LIST, q)); }
    @Override public LeaveRequestDto reviewLeave(LeaveReviewRequest r) throws NetworkClientException { return value(call(DormCommands.LEAVE_REVIEW, r), LeaveRequestDto.class); }
    @Override public DormPage<UtilityBillDto> bills(DormPageQuery q) throws NetworkClientException { return page(call(DormCommands.UTILITY_MINE, q)); }
    @Override public DormPage<UtilityBillDto> managerBills(UtilityBillQuery q) throws NetworkClientException { return page(call(DormCommands.UTILITY_MANAGER_LIST, q)); }
    @Override public UtilityBillDto payBill(UtilityPaymentRequest r) throws NetworkClientException { return value(call(DormCommands.UTILITY_PAY, r), UtilityBillDto.class); }
    @Override public DormPage<DormAnnouncementDto> announcements(DormPageQuery q) throws NetworkClientException { return page(call(DormCommands.ANNOUNCEMENT_LIST, q)); }
    @Override public DormAnnouncementDto saveAnnouncement(AnnouncementSaveRequest r) throws NetworkClientException { return value(call(DormCommands.ANNOUNCEMENT_SAVE, r), DormAnnouncementDto.class); }

    public void synchronizeSessionToken() { network.setSessionToken(session.isAuthenticated() ? session.getSessionToken() : null); }

    private Message call(String command, Serializable payload) throws NetworkClientException {
        if (!session.isAuthenticated()) throw new NetworkClientException(ResultCodes.UNAUTHORIZED, "请先登录");
        network.setSessionToken(session.getSessionToken());
        return network.request(command, payload);
    }

    /** 本人查询的学生范围由服务端会话确定，不接受请求中的学生 ID。 */
    private static LeaveQuery myLeaveQuery(LeaveQuery query) {
        if (query == null) return null;
        return new LeaveQuery(query.getPage(), query.getPageSize(), query.getStatus(),
                (Long) null, query.getStartDate(), query.getEndDate());
    }

    @SuppressWarnings("unchecked")
    private static <T> DormPage<T> page(Message response) throws NetworkClientException {
        if (response == null || !(response.getPayload() instanceof DormPage)) {
            throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "宿舍分页响应格式不正确");
        }
        return (DormPage<T>) response.getPayload();
    }

    private static <T> T value(Message response, Class<T> type) throws NetworkClientException {
        if (response == null || !type.isInstance(response.getPayload())) {
            throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "宿舍响应格式不正确");
        }
        return type.cast(response.getPayload());
    }
}
