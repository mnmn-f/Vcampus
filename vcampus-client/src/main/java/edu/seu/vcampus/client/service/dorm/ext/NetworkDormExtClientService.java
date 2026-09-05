package edu.seu.vcampus.client.service.dorm.ext;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;

import java.io.Serializable;

/** 宿舍扩展网络边界；与基础宿舍服务共享组合根中的 NetworkClientService。 */
public final class NetworkDormExtClientService implements DormExtClientService {
    private final NetworkClientService network;
    private final ClientSession session;

    public NetworkDormExtClientService(NetworkClientService network, ClientSession session) {
        if (network == null || session == null) throw new IllegalArgumentException("dorm extension client dependencies required");
        this.network = network;
        this.session = session;
    }

    @Override public DormPage<MeterReadingDto> meterReadings(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.METER_LIST, q)); }
    @Override public MeterReadingDto saveMeterReading(MeterReadingRequest r) throws NetworkClientException { return value(DormExtCommands.METER_SUBMIT, r, MeterReadingDto.class); }
    @Override public BillGenerateResultDto generateBills(BillGenerateRequest r) throws NetworkClientException { return value(DormExtCommands.BILL_GENERATE, r, BillGenerateResultDto.class); }
    @Override public WarningScanResultDto scanAbsences(WarningScanRequest r) throws NetworkClientException { return value(DormExtCommands.WARNING_SCAN, r, WarningScanResultDto.class); }
    @Override public DormPage<AbsenceWarningDto> warnings(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.WARNING_LIST, q)); }
    @Override public AbsenceWarningDto notifyWarning(WarningHandleRequest r) throws NetworkClientException { return value(DormExtCommands.WARNING_NOTIFY, r, AbsenceWarningDto.class); }
    @Override public AbsenceWarningDto verifyWarning(WarningHandleRequest r) throws NetworkClientException { return value(DormExtCommands.WARNING_VERIFY, r, AbsenceWarningDto.class); }
    @Override public WarningConfigDto warningConfig() throws NetworkClientException { return value(DormExtCommands.WARNING_CONFIG_GET, null, WarningConfigDto.class); }
    @Override public WarningConfigDto saveWarningConfig(WarningConfigRequest r) throws NetworkClientException { return value(DormExtCommands.WARNING_CONFIG_SET, r, WarningConfigDto.class); }
    @Override public VisitorRegistrationDto submitVisitor(VisitorRegistrationRequest r) throws NetworkClientException { return value(DormExtCommands.VISITOR_SUBMIT, r, VisitorRegistrationDto.class); }
    @Override public DormPage<VisitorRegistrationDto> ownVisitors(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.VISITOR_MINE, q)); }
    @Override public VisitorRegistrationDto cancelVisitor(VisitorAuditRequest r) throws NetworkClientException { return value(DormExtCommands.VISITOR_CANCEL, r, VisitorRegistrationDto.class); }
    @Override public DormPage<VisitorRegistrationDto> visitors(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.VISITOR_LIST, q)); }
    @Override public VisitorRegistrationDto auditVisitor(VisitorAuditRequest r) throws NetworkClientException { return value(DormExtCommands.VISITOR_AUDIT, r, VisitorRegistrationDto.class); }
    @Override public HygieneDetailDto submitHygiene(HygieneScoreSubmitRequest r) throws NetworkClientException { return value(DormExtCommands.HYGIENE_SUBMIT, r, HygieneDetailDto.class); }
    @Override public HygieneDetailDto hygieneDetail(HygieneDetailRequest r) throws NetworkClientException { return value(DormExtCommands.HYGIENE_DETAIL, r, HygieneDetailDto.class); }
    @Override public HygieneTaskGenerateResultDto generateHygieneTasks(HygieneTaskGenerateRequest r) throws NetworkClientException { return value(DormExtCommands.HYGIENE_TASK_GENERATE, r, HygieneTaskGenerateResultDto.class); }
    @Override public DormPage<HygieneTaskDto> hygieneTasks(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.HYGIENE_TASK_LIST, q)); }
    @Override public DormHomeSummaryDto homeSummary() throws NetworkClientException { return value(DormExtCommands.HOME_SUMMARY, null, DormHomeSummaryDto.class); }
    @Override @SuppressWarnings("unchecked")
    public java.util.List<RepairWorkerDto> repairWorkers() throws NetworkClientException {
        Object result = call(DormExtCommands.REPAIR_WORKERS, null).getPayload();
        if (!(result instanceof java.util.List)) throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "服务器返回数据类型不正确");
        return (java.util.List<RepairWorkerDto>) result;
    }
    @Override public RepairWorkOrderDto repairDetail(long orderId) throws NetworkClientException { return value(DormExtCommands.REPAIR_DETAIL, Long.valueOf(orderId), RepairWorkOrderDto.class); }
    @Override public RepairWorkOrderDto assignRepair(RepairAssignRequest r) throws NetworkClientException { return value(DormExtCommands.REPAIR_ASSIGN, r, RepairWorkOrderDto.class); }
    @Override public RepairWorkOrderDto reviewRepair(RepairWorkRequest r) throws NetworkClientException { return value(DormExtCommands.REPAIR_REVIEW, r, RepairWorkOrderDto.class); }
    @Override public DormPage<RepairWorkOrderDto> repairQueue(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.REPAIR_QUEUE, q)); }
    @Override public DormPage<RepairWorkOrderDto> repairAssigned(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.REPAIR_ASSIGNED, q)); }
    @Override public DormPage<RepairWorkOrderDto> repairHistory(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.REPAIR_HISTORY, q)); }
    @Override public RepairWorkOrderDto acceptRepair(RepairWorkRequest r) throws NetworkClientException { return value(DormExtCommands.REPAIR_ACCEPT, r, RepairWorkOrderDto.class); }
    @Override public RepairWorkOrderDto startRepair(RepairWorkRequest r) throws NetworkClientException { return value(DormExtCommands.REPAIR_START, r, RepairWorkOrderDto.class); }
    @Override public RepairWorkOrderDto finishRepair(RepairWorkRequest r) throws NetworkClientException { return value(DormExtCommands.REPAIR_FINISH, r, RepairWorkOrderDto.class); }
    @Override public StayStatusDto myStayStatus() throws NetworkClientException { return value(DormExtCommands.STAY_MINE, null, StayStatusDto.class); }
    @Override public DormPage<StayStatusDto> stayStatuses() throws NetworkClientException { return page(call(DormExtCommands.STAY_LIST, null)); }
    @Override public DormPage<AccessRecordExtDto> myAccessRecords(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.ACCESS_MINE, q)); }
    @Override public AccessPolicyDto accessPolicy() throws NetworkClientException { return value(DormExtCommands.ACCESS_POLICY_GET, null, AccessPolicyDto.class); }
    @Override public AccessPolicyDto saveAccessPolicy(AccessPolicyRequest r) throws NetworkClientException { return value(DormExtCommands.ACCESS_POLICY_SET, r, AccessPolicyDto.class); }
    @Override public RepairEntryPermitDto setRepairPermit(RepairEntryPermitRequest r) throws NetworkClientException { return value(DormExtCommands.REPAIR_PERMIT_SET, r, RepairEntryPermitDto.class); }
    @Override public DormPage<RepairEntryPermitDto> myRepairPermits(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.REPAIR_PERMIT_MINE, q)); }
    @Override public Long deleteRoom(RoomDeleteRequest r) throws NetworkClientException { return value(DormExtCommands.ROOM_DELETE, r, Long.class); }
    @Override public DormPage<NoticeExtraDto> myNotices(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.NOTICE_MINE, q)); }
    @Override public DormPage<NoticeExtraDto> notices(DormPageQuery q) throws NetworkClientException { return page(call(DormExtCommands.NOTICE_LIST, q)); }
    @Override public NoticeExtraDto saveNoticeExtra(NoticeExtraRequest r) throws NetworkClientException { return value(DormExtCommands.NOTICE_EXTRA_SET, r, NoticeExtraDto.class); }
    @Override public DormExtStatusDto status() throws NetworkClientException { return value(DormExtCommands.STATUS, null, DormExtStatusDto.class); }
    @Override public DormExtStatusDto runScheduledTask(String task) throws NetworkClientException { return value(DormExtCommands.SCHEDULER_RUN, task, DormExtStatusDto.class); }

    private Message call(String command, Serializable payload) throws NetworkClientException {
        network.setSessionToken(session.isAuthenticated() ? session.getSessionToken() : null);
        return network.request(command, payload);
    }

    private <T> T value(String command, Serializable payload, Class<T> type) throws NetworkClientException {
        Object result = call(command, payload).getPayload();
        if (!type.isInstance(result)) throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "服务器返回数据类型不正确");
        return type.cast(result);
    }

    @SuppressWarnings("unchecked")
    private static <T> DormPage<T> page(Message response) throws NetworkClientException {
        if (!(response.getPayload() instanceof DormPage)) throw new NetworkClientException(ResultCodes.INTERNAL_ERROR, "服务器返回分页数据类型不正确");
        return (DormPage<T>) response.getPayload();
    }
}
