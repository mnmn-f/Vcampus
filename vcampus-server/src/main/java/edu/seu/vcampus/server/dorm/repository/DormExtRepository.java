package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneItemScoreDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/**
 * 宿舍扩展模块的持久化边界。
 *
 * <p>刻意不继承 {@link DormRepository}：扩展能力独立演进，既有仓储组合不受影响。
 * 账单生成只暴露若干原语，编排与分摊算法留在服务层，便于用内存实现做单测。</p>
 */
public interface DormExtRepository {
    /** 按房间、楼栋或关键字分页查询抄表读数。 */
    DormPage<MeterReadingDto> listMeterReadings(Connection connection, DormPageQuery query)
            throws SQLException;

    /** 录入抄表读数；同房间同账期已存在则更新，已生成账单的读数拒绝改动。 */
    MeterReadingDto saveMeterReading(Connection connection, MeterReadingRequest request,
                                     long actorUserId) throws SQLException;

    /** 指定账期内尚未出账的读数；房间为空表示全部房间。 */
    List<MeterReadingDto> pendingReadings(Connection connection, Long roomId,
                                          LocalDate periodStart, LocalDate periodEnd)
            throws SQLException;

    /** 房间当前在住的学生用户号，按用户号升序，保证分摊顺序稳定可复现。 */
    List<Long> activeResidents(Connection connection, long roomId) throws SQLException;

    /** 该房间该账期是否已经出过账。 */
    boolean billExists(Connection connection, long roomId, LocalDate periodStart,
                       LocalDate periodEnd) throws SQLException;

    /** 写入房间账单，返回账单编号。 */
    long createBill(Connection connection, MeterReadingDto reading, BigDecimal totalAmount,
                    LocalDateTime dueAt, long actorUserId) throws SQLException;

    /** 写入一条学生分摊记录。 */
    void createAllocation(Connection connection, long billId, long studentUserId,
                          BigDecimal amount) throws SQLException;

    /** 回填读数的账单编号，把读数锁定为不可再改。 */
    void linkReadingToBill(Connection connection, long readingId, long billId)
            throws SQLException;

    // ---- 未归预警 ----

    /** 全部在住学生及其最近一次进出时间，作为未归扫描的输入。 */
    List<ResidentAbsenceSnapshot> residentsForScan(Connection connection) throws SQLException;

    /** 该生在指定日期是否有已批准的请假。 */
    boolean hasApprovedLeave(Connection connection, long studentUserId, LocalDate date)
            throws SQLException;

    /** 写入或更新一条预警；同一学生同一扫描日唯一，重复扫描按更新处理。 */
    AbsenceWarningDto saveWarning(Connection connection, long studentUserId, long roomId,
                                  LocalDate scanDate, LocalDateTime lastLeaveAt,
                                  int absenceDays, String warningLevel) throws SQLException;

    /** 预警分页查询。 */
    DormPage<AbsenceWarningDto> listWarnings(Connection connection, DormPageQuery query)
            throws SQLException;

    /** 按编号读取一条预警。 */
    AbsenceWarningDto findWarning(Connection connection, long warningId) throws SQLException;

    /** 更新预警的处理状态。 */
    AbsenceWarningDto updateWarningStatus(Connection connection, long warningId, String status,
                                          Long teacherUserId, LocalDateTime notifiedAt,
                                          String note) throws SQLException;

    /** 读取阈值配置。 */
    WarningConfigDto loadWarningConfig(Connection connection) throws SQLException;

    /** 保存阈值配置。 */
    WarningConfigDto saveWarningConfig(Connection connection, WarningConfigRequest request,
                                       long actorUserId) throws SQLException;

    // ---- 外来人员登记 ----

    /** 学生当前在住的房间编号；没有有效住宿记录时返回 null。 */
    Long activeRoomOf(Connection connection, long studentUserId) throws SQLException;

    /** 写入一条来访登记，返回落库后的视图。 */
    VisitorRegistrationDto createVisitor(Connection connection, long studentUserId, long roomId,
                                         VisitorRegistrationRequest request) throws SQLException;

    /** 来访登记分页查询；studentUserId 非空表示只看本人。 */
    DormPage<VisitorRegistrationDto> listVisitors(Connection connection, DormPageQuery query,
                                                  Long studentUserId) throws SQLException;

    /** 按编号读取一条来访登记。 */
    VisitorRegistrationDto findVisitor(Connection connection, long registrationId)
            throws SQLException;

    /** 更新来访登记的审核状态；撤销时 auditorId 与 auditedAt 传 null。 */
    VisitorRegistrationDto updateVisitorStatus(Connection connection, long registrationId,
                                               String status, Long auditorId,
                                               LocalDateTime auditedAt, String remark)
            throws SQLException;

    // ---- 卫生分项与检查任务 ----

    /** 写入一次卫生检查主记录（V1 的 hygiene_inspections），返回编号。 */
    long createInspection(Connection connection, long roomId, long inspectorId,
                          LocalDateTime inspectedAt, BigDecimal totalScore, String result,
                          String status, String issueDescription) throws SQLException;

    /** 写入该次检查的五项分项得分。 */
    void saveItemScores(Connection connection, long inspectionId,
                        List<HygieneItemScoreDto> items) throws SQLException;

    /** 读取一次检查的完整视图（含分项）；不存在返回 null。 */
    HygieneDetailDto findInspectionDetail(Connection connection, long inspectionId)
            throws SQLException;

    /** 可参与周检查的房间编号；楼栋为空表示全部。 */
    List<Long> roomsForWeeklyTask(Connection connection, Long buildingId) throws SQLException;

    /** 建一条检查任务；同房间同日期同类型已存在时返回 false，不重复建。 */
    boolean createTaskIfAbsent(Connection connection, long roomId, String taskType,
                               LocalDate planDate, Long sourceInspectionId) throws SQLException;

    /** 提交检查后把该房间到期的待办任务标记完成。 */
    int markTasksDone(Connection connection, long roomId, LocalDate onOrBefore, long inspectionId)
            throws SQLException;

    /** 检查任务分页查询。 */
    DormPage<HygieneTaskDto> listTasks(Connection connection, DormPageQuery query)
            throws SQLException;

    // ---- 在宿状态、门禁策略、入内许可与房间删除 ----

    /**
     * 在住学生的在宿状态。
     *
     * <p>studentUserId 非空表示只查一人。请假豁免由调用方逐条判定，因此这里只
     * 返回房间与最近进出时间。</p>
     */
    List<StayStatusDto> stayStatusRows(Connection connection, Long studentUserId)
            throws SQLException;

    AccessPolicyDto loadAccessPolicy(Connection connection) throws SQLException;

    AccessPolicyDto saveAccessPolicy(Connection connection, AccessPolicyRequest request,
                                     long actorUserId) throws SQLException;

    /** 本人进出记录；晚归按传入策略实时判定。 */
    DormPage<AccessRecordExtDto> listAccessRecords(Connection connection, long studentUserId,
                                                   DormPageQuery query, AccessPolicyDto policy)
            throws SQLException;

    /** 报修单的提交人；不存在返回 null。 */
    Long repairReporterOf(Connection connection, long repairOrderId) throws SQLException;

    /** 报修单提交人账号里登记的手机号；没登记或工单不存在返回 null。 */
    String reporterPhoneOf(Connection connection, long repairOrderId) throws SQLException;

    RepairEntryPermitDto saveRepairPermit(Connection connection, RepairEntryPermitRequest request,
                                          long actorUserId) throws SQLException;

    DormPage<RepairEntryPermitDto> listRepairPermits(Connection connection, long studentUserId,
                                                     DormPageQuery query) throws SQLException;

    /** 房间当前在住人数。 */
    int activeResidentCount(Connection connection, long roomId) throws SQLException;

    /** 房间被历史数据引用的条数（住宿、账单、检查、工单、抄表、任务、预警、来访）。 */
    int roomReferenceCount(Connection connection, long roomId) throws SQLException;

    /** 删除房间及其床位；调用方须先确认没有在住和历史引用。 */
    void deleteRoom(Connection connection, long roomId) throws SQLException;

    // ---- 公告类型、范围与置顶 ----

    /**
     * 带扩展属性的宿舍公告分页。
     *
     * <p>{@code manageView} 为真时返回全部 DORM 公告（含草稿）且不做范围过滤；
     * 为假时只返回已发布且在有效期内、可见范围命中 {@code viewerRoomId} 的公告，
     * {@code viewerRoomId} 为空表示该生当前没有住宿记录，只能看到全体公告。</p>
     */
    DormPage<NoticeExtraDto> listNotices(Connection connection, DormPageQuery query,
                                         boolean manageView, Long viewerRoomId)
            throws SQLException;

    /** 按编号读取一条宿舍公告；不存在或不属于 DORM 模块返回 null。 */
    NoticeExtraDto findNotice(Connection connection, long announcementId) throws SQLException;

    /** 写入或更新公告的类型、范围与置顶；不触碰公告正文。 */
    NoticeExtraDto saveNoticeExtra(Connection connection, NoticeExtraRequest request,
                                   long actorUserId) throws SQLException;

    // ---- 定时任务专用 ----

    /**
     * 把已过期的宿舍公告置为 EXPIRED，返回受影响条数。
     *
     * <p>只动 {@code module_code = 'DORM'} 且已发布、已过 {@code expire_at} 的记录，
     * 其余模块的公告完全不受影响。</p>
     */
    int expireDormAnnouncements(Connection connection, LocalDateTime now) throws SQLException;

    /** 指定日期及之前仍未处理（PENDING）的严重未归预警，供每日提醒汇总。 */
    List<AbsenceWarningDto> pendingSevereWarnings(Connection connection, LocalDate onOrBefore)
            throws SQLException;
}
