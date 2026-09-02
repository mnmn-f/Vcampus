package edu.seu.vcampus.client.service.dorm.ext;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.network.SocketClientGateway;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneScoreSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RoomDeleteRequest;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorAuditRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningHandleRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanResultDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;

import java.io.Serializable;

/**
 * 宿舍扩展命令的 Socket 实现。
 *
 * <p>自建一条到服务端的连接，而不是复用 {@code ClientBusinessServices} 持有的那条：
 * 后者是客户端组合根，多个模块的同学都会改它，把宿舍扩展塞进去会明显抬高合并冲突
 * 的概率。代价是宿管进入宿舍页面时多占一条 TCP 连接（服务端默认容量 32），
 * 换来的是整个扩展模块只需要在 {@code RealDormPage} 里加几行接线。</p>
 *
 * <p>会话令牌每次请求前从 {@link ClientSession} 同步，因此切换职责或重新登录后
 * 依然使用当前有效的令牌。</p>
 */
public final class NetworkDormExtClientService implements DormExtClientService {
    // 页面每次被导航到都会重建，但连接必须复用：否则来回切换几次就会耗光服务端
    // 的连接额度。桌面客户端同时只有一个登录会话，用会话实例做键即可。
    private static ClientSession cachedSession;
    private static DormExtClientService cached;

    private final NetworkClientService network;
    private final ClientSession session;

    private NetworkDormExtClientService(NetworkClientService network, ClientSession session) {
        this.network = network;
        this.session = session;
    }

    /** 按与 AppLauncher 相同的系统属性建立连接；同一会话复用同一条。 */
    public static synchronized DormExtClientService create(ClientSession session) {
        if (session == null) throw new IllegalArgumentException("session 不能为空");
        if (cached != null && cachedSession == session) return cached;
        String host = System.getProperty("vcampus.server.host", "127.0.0.1").trim();
        int port = positive("vcampus.server.port", 8888);
        SocketClientGateway gateway = new SocketClientGateway(host, port,
                positive("vcampus.server.connect-timeout", 5000),
                positive("vcampus.server.read-timeout", 5000));
        cached = new NetworkDormExtClientService(new NetworkClientService(gateway), session);
        cachedSession = session;
        return cached;
    }

    @Override
    public DormPage<MeterReadingDto> meterReadings(DormPageQuery query) throws NetworkClientException {
        return cast(call(DormExtCommands.METER_LIST, query));
    }

    @Override
    public MeterReadingDto saveMeterReading(MeterReadingRequest request) throws NetworkClientException {
        return (MeterReadingDto) call(DormExtCommands.METER_SUBMIT, request).getPayload();
    }

    @Override
    public BillGenerateResultDto generateBills(BillGenerateRequest request) throws NetworkClientException {
        return (BillGenerateResultDto) call(DormExtCommands.BILL_GENERATE, request).getPayload();
    }

    @Override
    public WarningScanResultDto scanAbsences(WarningScanRequest request) throws NetworkClientException {
        return (WarningScanResultDto) call(DormExtCommands.WARNING_SCAN, request).getPayload();
    }

    @Override
    public DormPage<AbsenceWarningDto> warnings(DormPageQuery query) throws NetworkClientException {
        return cast(call(DormExtCommands.WARNING_LIST, query));
    }

    @Override
    public AbsenceWarningDto notifyWarning(WarningHandleRequest request) throws NetworkClientException {
        return (AbsenceWarningDto) call(DormExtCommands.WARNING_NOTIFY, request).getPayload();
    }

    @Override
    public AbsenceWarningDto verifyWarning(WarningHandleRequest request) throws NetworkClientException {
        return (AbsenceWarningDto) call(DormExtCommands.WARNING_VERIFY, request).getPayload();
    }

    @Override
    public WarningConfigDto warningConfig() throws NetworkClientException {
        return (WarningConfigDto) call(DormExtCommands.WARNING_CONFIG_GET, null).getPayload();
    }

    @Override
    public WarningConfigDto saveWarningConfig(WarningConfigRequest request) throws NetworkClientException {
        return (WarningConfigDto) call(DormExtCommands.WARNING_CONFIG_SET, request).getPayload();
    }

    @Override
    public VisitorRegistrationDto submitVisitor(VisitorRegistrationRequest request)
            throws NetworkClientException {
        return (VisitorRegistrationDto) call(DormExtCommands.VISITOR_SUBMIT, request).getPayload();
    }

    @Override
    public DormPage<VisitorRegistrationDto> ownVisitors(DormPageQuery query)
            throws NetworkClientException {
        return cast(call(DormExtCommands.VISITOR_MINE, query));
    }

    @Override
    public VisitorRegistrationDto cancelVisitor(VisitorAuditRequest request)
            throws NetworkClientException {
        return (VisitorRegistrationDto) call(DormExtCommands.VISITOR_CANCEL, request).getPayload();
    }

    @Override
    public DormPage<VisitorRegistrationDto> visitors(DormPageQuery query)
            throws NetworkClientException {
        return cast(call(DormExtCommands.VISITOR_LIST, query));
    }

    @Override
    public VisitorRegistrationDto auditVisitor(VisitorAuditRequest request)
            throws NetworkClientException {
        return (VisitorRegistrationDto) call(DormExtCommands.VISITOR_AUDIT, request).getPayload();
    }

    @Override
    public HygieneDetailDto submitHygiene(HygieneScoreSubmitRequest request)
            throws NetworkClientException {
        return (HygieneDetailDto) call(DormExtCommands.HYGIENE_SUBMIT, request).getPayload();
    }

    @Override
    public HygieneDetailDto hygieneDetail(HygieneDetailRequest request)
            throws NetworkClientException {
        return (HygieneDetailDto) call(DormExtCommands.HYGIENE_DETAIL, request).getPayload();
    }

    @Override
    public HygieneTaskGenerateResultDto generateHygieneTasks(HygieneTaskGenerateRequest request)
            throws NetworkClientException {
        return (HygieneTaskGenerateResultDto)
                call(DormExtCommands.HYGIENE_TASK_GENERATE, request).getPayload();
    }

    @Override
    public DormPage<HygieneTaskDto> hygieneTasks(DormPageQuery query)
            throws NetworkClientException {
        return cast(call(DormExtCommands.HYGIENE_TASK_LIST, query));
    }

    @Override
    public StayStatusDto myStayStatus() throws NetworkClientException {
        return (StayStatusDto) call(DormExtCommands.STAY_MINE, null).getPayload();
    }

    @Override
    public DormPage<StayStatusDto> stayStatuses() throws NetworkClientException {
        return cast(call(DormExtCommands.STAY_LIST, null));
    }

    @Override
    public DormPage<AccessRecordExtDto> myAccessRecords(DormPageQuery query)
            throws NetworkClientException {
        return cast(call(DormExtCommands.ACCESS_MINE, query));
    }

    @Override
    public AccessPolicyDto accessPolicy() throws NetworkClientException {
        return (AccessPolicyDto) call(DormExtCommands.ACCESS_POLICY_GET, null).getPayload();
    }

    @Override
    public AccessPolicyDto saveAccessPolicy(AccessPolicyRequest request)
            throws NetworkClientException {
        return (AccessPolicyDto) call(DormExtCommands.ACCESS_POLICY_SET, request).getPayload();
    }

    @Override
    public RepairEntryPermitDto setRepairPermit(RepairEntryPermitRequest request)
            throws NetworkClientException {
        return (RepairEntryPermitDto) call(DormExtCommands.REPAIR_PERMIT_SET, request).getPayload();
    }

    @Override
    public DormPage<RepairEntryPermitDto> myRepairPermits(DormPageQuery query)
            throws NetworkClientException {
        return cast(call(DormExtCommands.REPAIR_PERMIT_MINE, query));
    }

    @Override
    public Long deleteRoom(RoomDeleteRequest request) throws NetworkClientException {
        return (Long) call(DormExtCommands.ROOM_DELETE, request).getPayload();
    }

    @Override
    public DormPage<NoticeExtraDto> myNotices(DormPageQuery query) throws NetworkClientException {
        return cast(call(DormExtCommands.NOTICE_MINE, query));
    }

    @Override
    public DormPage<NoticeExtraDto> notices(DormPageQuery query) throws NetworkClientException {
        return cast(call(DormExtCommands.NOTICE_LIST, query));
    }

    @Override
    public NoticeExtraDto saveNoticeExtra(NoticeExtraRequest request)
            throws NetworkClientException {
        return (NoticeExtraDto) call(DormExtCommands.NOTICE_EXTRA_SET, request).getPayload();
    }

    @Override
    public DormExtStatusDto status() throws NetworkClientException {
        return (DormExtStatusDto) call(DormExtCommands.STATUS, null).getPayload();
    }

    @Override
    public DormExtStatusDto runScheduledTask(String taskName) throws NetworkClientException {
        return (DormExtStatusDto) call(DormExtCommands.SCHEDULER_RUN, taskName).getPayload();
    }

    private Message call(String command, Serializable payload) throws NetworkClientException {
        network.setSessionToken(session.isAuthenticated() ? session.getSessionToken() : null);
        return network.request(command, payload);
    }

    @SuppressWarnings("unchecked")
    private static <T> DormPage<T> cast(Message response) {
        return (DormPage<T>) response.getPayload();
    }

    private static int positive(String key, int fallback) {
        String value = System.getProperty(key, Integer.toString(fallback)).trim();
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
