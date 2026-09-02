package edu.seu.vcampus.client.service.dorm.ext;

import edu.seu.vcampus.client.network.NetworkClientException;
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

/** 宿舍扩展能力的客户端网络边界，与既有 DormClientService 并列，不修改后者。 */
public interface DormExtClientService {
    DormPage<MeterReadingDto> meterReadings(DormPageQuery query) throws NetworkClientException;

    MeterReadingDto saveMeterReading(MeterReadingRequest request) throws NetworkClientException;

    BillGenerateResultDto generateBills(BillGenerateRequest request) throws NetworkClientException;

    WarningScanResultDto scanAbsences(WarningScanRequest request) throws NetworkClientException;

    DormPage<AbsenceWarningDto> warnings(DormPageQuery query) throws NetworkClientException;

    AbsenceWarningDto notifyWarning(WarningHandleRequest request) throws NetworkClientException;

    AbsenceWarningDto verifyWarning(WarningHandleRequest request) throws NetworkClientException;

    WarningConfigDto warningConfig() throws NetworkClientException;

    WarningConfigDto saveWarningConfig(WarningConfigRequest request) throws NetworkClientException;

    VisitorRegistrationDto submitVisitor(VisitorRegistrationRequest request)
            throws NetworkClientException;

    DormPage<VisitorRegistrationDto> ownVisitors(DormPageQuery query) throws NetworkClientException;

    VisitorRegistrationDto cancelVisitor(VisitorAuditRequest request) throws NetworkClientException;

    DormPage<VisitorRegistrationDto> visitors(DormPageQuery query) throws NetworkClientException;

    VisitorRegistrationDto auditVisitor(VisitorAuditRequest request) throws NetworkClientException;

    HygieneDetailDto submitHygiene(HygieneScoreSubmitRequest request) throws NetworkClientException;

    HygieneDetailDto hygieneDetail(HygieneDetailRequest request) throws NetworkClientException;

    HygieneTaskGenerateResultDto generateHygieneTasks(HygieneTaskGenerateRequest request)
            throws NetworkClientException;

    DormPage<HygieneTaskDto> hygieneTasks(DormPageQuery query) throws NetworkClientException;

    // ---- 在宿状态、门禁、入内许可与房间删除 ----

    /** 学生查看本人在宿状态。 */
    StayStatusDto myStayStatus() throws NetworkClientException;

    /** 宿管查看全部在住学生的在宿状态。 */
    DormPage<StayStatusDto> stayStatuses() throws NetworkClientException;

    /** 学生查看本人进出记录；晚归标记由服务端按当前门禁策略实时判定。 */
    DormPage<AccessRecordExtDto> myAccessRecords(DormPageQuery query) throws NetworkClientException;

    AccessPolicyDto accessPolicy() throws NetworkClientException;

    AccessPolicyDto saveAccessPolicy(AccessPolicyRequest request) throws NetworkClientException;

    /** 学生对自己的报修单授权/撤销维修人员入内。 */
    RepairEntryPermitDto setRepairPermit(RepairEntryPermitRequest request)
            throws NetworkClientException;

    DormPage<RepairEntryPermitDto> myRepairPermits(DormPageQuery query)
            throws NetworkClientException;

    /** 删除空置且无历史引用的房间，返回被删除的房间编号。 */
    Long deleteRoom(RoomDeleteRequest request) throws NetworkClientException;

    // ---- 公告类型、范围与置顶 ----

    /** 学生查看面向自己的宿舍公告。 */
    DormPage<NoticeExtraDto> myNotices(DormPageQuery query) throws NetworkClientException;

    /** 宿管查看全部宿舍公告及其扩展属性。 */
    DormPage<NoticeExtraDto> notices(DormPageQuery query) throws NetworkClientException;

    /** 宿管设置公告的类型、可见范围与置顶。 */
    NoticeExtraDto saveNoticeExtra(NoticeExtraRequest request) throws NetworkClientException;

    // ---- 服务端运行状态 ----

    /** 模块版本、调度器状态与各定时任务的最近一次结果。 */
    DormExtStatusDto status() throws NetworkClientException;

    /** 立刻执行一个定时任务并返回刷新后的状态；taskName 为空表示全部。 */
    DormExtStatusDto runScheduledTask(String taskName) throws NetworkClientException;
}
