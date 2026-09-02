package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RoomDeleteRequest;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneScoreSubmitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorAuditRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningHandleRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanResultDto;
import edu.seu.vcampus.server.dorm.repository.ResidentAbsenceSnapshot;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import edu.seu.vcampus.server.security.SessionContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/**
 * 宿舍扩展模块的服务门面。
 *
 * <p>放在既有 {@code dorm.service} 包内，直接复用包内的 {@code DormServiceSupport}
 * 事务与鉴权基类；权限一律复用已登记的宿舍权限，不新增 {@code Permission} 枚举值，
 * 因此 {@code Permission} 和 {@code RolePolicy} 都不需要改动。</p>
 */
public final class DormExtService extends DormServiceSupport {
    /** 随阶段推进更新，便于在监控界面确认服务端跑的是哪一版。 */
    public static final String MODULE_VERSION = "dorm-ext/P6";

    /** 未指定缴费截止时间时，默认给账期结束后 15 天。 */
    private static final int DEFAULT_DUE_DAYS = 15;

    private final DormExtRepository repository;
    /** 定时任务台账；调度器写、status() 读，未启动调度器时保持空状态。 */
    private final DormSchedulerStatus schedulerStatus = new DormSchedulerStatus();
    /** 调度器启动后挂上来；未启动时为 null，手动触发会明确报错而不是静默成功。 */
    private volatile DormTaskRunner taskRunner;

    public DormExtService(DormExtRepository repository, TransactionManager transactions) {
        super(transactions);
        if (repository == null) {
            throw new IllegalArgumentException("repository is required");
        }
        this.repository = repository;
    }

    /** 无事务构造；单测使用内存仓储时走这条路径。 */
    public DormExtService(DormExtRepository repository) { this(repository, null); }

    public DormPage<MeterReadingDto> meterReadings(final SessionContext session,
                                                   final DormPageQuery query) {
        require(session, Permission.DORM_GOVERN);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<MeterReadingDto>>() {
            @Override
            public DormPage<MeterReadingDto> run(Connection c) throws Exception {
                return repository.listMeterReadings(c, q);
            }
        });
    }

    public MeterReadingDto saveMeterReading(final SessionContext session,
                                            final MeterReadingRequest request) {
        require(session, Permission.DORM_GOVERN);
        validate(request);
        return execute(new Work<MeterReadingDto>() {
            @Override
            public MeterReadingDto run(Connection c) throws Exception {
                return repository.saveMeterReading(c, request, session.getUserId());
            }
        });
    }

    /**
     * 把指定账期内待出账的读数生成房间账单与学生分摊。
     *
     * <p>整批在同一个事务里完成：任何一个房间写失败都会整体回滚，不会留下
     * 「账单已建、分摊没建」的半截数据。无人在住、金额过小或已出过账的房间
     * 跳过并记入 notes，不影响其余房间。</p>
     */
    public BillGenerateResultDto generateBills(final SessionContext session,
                                               final BillGenerateRequest request) {
        require(session, Permission.DORM_GOVERN);
        validatePeriod(request);
        return execute(new Work<BillGenerateResultDto>() {
            @Override
            public BillGenerateResultDto run(Connection c) throws Exception {
                return generate(c, request, session.getUserId());
            }
        });
    }

    private BillGenerateResultDto generate(Connection c, BillGenerateRequest request, long actor)
            throws Exception {
        LocalDateTime dueAt = request.getDueAt() != null ? request.getDueAt()
                : request.getPeriodEnd().plusDays(DEFAULT_DUE_DAYS).atTime(23, 59);
        List<MeterReadingDto> pending = repository.pendingReadings(c, request.getRoomId(),
                request.getPeriodStart(), request.getPeriodEnd());
        if (pending.isEmpty()) {
            throw new DormException(DormExtCommands.NO_PENDING_READING,
                    "该账期没有待出账的抄表读数");
        }
        List<String> notes = new ArrayList<String>();
        BigDecimal total = BigDecimal.ZERO;
        int bills = 0;
        int shares = 0;
        int skipped = 0;
        for (MeterReadingDto reading : pending) {
            String label = reading.getBuildingCode() + " " + reading.getRoomNo();
            if (repository.billExists(c, reading.getRoomId(), reading.getPeriodStart(),
                    reading.getPeriodEnd())) {
                skipped++;
                notes.add(label + "：该账期已有账单，跳过");
                continue;
            }
            List<Long> residents = repository.activeResidents(c, reading.getRoomId());
            if (residents.isEmpty()) {
                skipped++;
                notes.add(label + "：无在住学生，跳过");
                continue;
            }
            BigDecimal amount = reading.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
            if (!DormBillSplit.isSplittable(amount, residents.size())) {
                skipped++;
                notes.add(label + "：应缴 " + amount + " 元不足以按 " + residents.size()
                        + " 人分摊，跳过");
                continue;
            }
            long billId = repository.createBill(c, reading, amount, dueAt, actor);
            List<BigDecimal> split = DormBillSplit.split(amount, residents.size());
            for (int i = 0; i < residents.size(); i++) {
                repository.createAllocation(c, billId, residents.get(i).longValue(), split.get(i));
                shares++;
            }
            repository.linkReadingToBill(c, reading.getId(), billId);
            bills++;
            total = total.add(amount);
            notes.add(label + "：账单 " + amount + " 元，由 " + residents.size() + " 人分摊");
        }
        return new BillGenerateResultDto(bills, shares, skipped, total, notes);
    }

    private static void validatePeriod(BillGenerateRequest request) {
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "出账参数不能为空");
        }
        if (request.getRoomId() != null) {
            requireId(request.getRoomId().longValue(), "房间编号");
        }
        LocalDate start = request.getPeriodStart();
        LocalDate end = request.getPeriodEnd();
        if (start == null || end == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "账期起止日期不能为空");
        }
        if (end.isBefore(start)) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "账期结束日期不能早于开始日期");
        }
    }

    // ================= 未归预警 =================

    /**
     * 扫描全部在住学生，按门禁流水计算连续未归天数并按阈值分级写入预警。
     *
     * <p>同一学生同一扫描日只留一条，重复扫描按更新处理，因此定时任务和宿管手动
     * 触发可以任意混用，不会产生重复告警。</p>
     */
    public WarningScanResultDto scanAbsences(final SessionContext session,
                                             final WarningScanRequest request) {
        require(session, Permission.DORM_GOVERN);
        return execute(new Work<WarningScanResultDto>() {
            @Override
            public WarningScanResultDto run(Connection c) throws Exception {
                return scan(c, request == null ? null : request.getScanDate());
            }
        });
    }

    private WarningScanResultDto scan(Connection c, LocalDate requested) throws Exception {
        LocalDate scanDate = requested == null ? LocalDate.now() : requested;
        WarningConfigDto config = repository.loadWarningConfig(c);
        List<ResidentAbsenceSnapshot> residents = repository.residentsForScan(c);
        int normal = 0;
        int severe = 0;
        int exempt = 0;
        for (ResidentAbsenceSnapshot resident : residents) {
            int days = DormAbsenceRules.absenceDays(resident.getLastExitAt(),
                    resident.getLastEntryAt(), scanDate);
            if (!DormAbsenceRules.shouldWarn(days, config.getWarnDays())) continue;
            boolean onLeave = config.isExemptOnLeave()
                    && repository.hasApprovedLeave(c, resident.getStudentUserId(), scanDate);
            String level = DormAbsenceRules.level(days, config.getNotifyDays(), onLeave);
            repository.saveWarning(c, resident.getStudentUserId(), resident.getRoomId(),
                    scanDate, resident.getLastExitAt(), days, level);
            if (AbsenceWarningDto.LEVEL_EXEMPT.equals(level)) exempt++;
            else if (AbsenceWarningDto.LEVEL_SEVERE.equals(level)) severe++;
            else normal++;
        }
        return new WarningScanResultDto(scanDate, residents.size(), normal, severe, exempt);
    }

    public DormPage<AbsenceWarningDto> warnings(final SessionContext session,
                                                final DormPageQuery query) {
        require(session, Permission.DORM_GOVERN);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<AbsenceWarningDto>>() {
            @Override
            public DormPage<AbsenceWarningDto> run(Connection c) throws Exception {
                return repository.listWarnings(c, q);
            }
        });
    }

    /** 通知辅导员；接收人必须显式给出，系统里没有学生到辅导员的映射。 */
    public AbsenceWarningDto notifyWarning(final SessionContext session,
                                           final WarningHandleRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "处理参数不能为空");
        }
        requireId(request.getWarningId(), "预警编号");
        if (request.getTeacherUserId() == null || request.getTeacherUserId().longValue() <= 0L) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "请指定要通知的辅导员");
        }
        return execute(new Work<AbsenceWarningDto>() {
            @Override
            public AbsenceWarningDto run(Connection c) throws Exception {
                AbsenceWarningDto current = requireWarning(c, request.getWarningId());
                if (AbsenceWarningDto.STATUS_VERIFIED.equals(current.getHandleStatus())) {
                    throw new DormException(DormExtCommands.WARNING_INVALID_STATE,
                            "该预警已核实，无需再通知");
                }
                return repository.updateWarningStatus(c, request.getWarningId(),
                        AbsenceWarningDto.STATUS_NOTIFIED, request.getTeacherUserId(),
                        LocalDateTime.now(), request.getNote());
            }
        });
    }

    /** 核实完毕，预警收口。 */
    public AbsenceWarningDto verifyWarning(final SessionContext session,
                                           final WarningHandleRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "处理参数不能为空");
        }
        requireId(request.getWarningId(), "预警编号");
        return execute(new Work<AbsenceWarningDto>() {
            @Override
            public AbsenceWarningDto run(Connection c) throws Exception {
                AbsenceWarningDto current = requireWarning(c, request.getWarningId());
                return repository.updateWarningStatus(c, request.getWarningId(),
                        AbsenceWarningDto.STATUS_VERIFIED, current.getNotifiedTeacherId(),
                        current.getNotifiedAt(), request.getNote());
            }
        });
    }

    public WarningConfigDto warningConfig(final SessionContext session) {
        require(session, Permission.DORM_GOVERN);
        return execute(new Work<WarningConfigDto>() {
            @Override
            public WarningConfigDto run(Connection c) throws Exception {
                return repository.loadWarningConfig(c);
            }
        });
    }

    public WarningConfigDto saveWarningConfig(final SessionContext session,
                                              final WarningConfigRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "阈值参数不能为空");
        }
        if (request.getWarnDays() <= 0) {
            throw new DormException(DormExtCommands.CONFIG_INVALID, "预警天数必须大于零");
        }
        if (request.getNotifyDays() < request.getWarnDays()) {
            throw new DormException(DormExtCommands.CONFIG_INVALID,
                    "通知天数不能小于预警天数");
        }
        return execute(new Work<WarningConfigDto>() {
            @Override
            public WarningConfigDto run(Connection c) throws Exception {
                return repository.saveWarningConfig(c, request, session.getUserId());
            }
        });
    }

    private AbsenceWarningDto requireWarning(Connection c, long warningId) throws Exception {
        AbsenceWarningDto current = repository.findWarning(c, warningId);
        if (current == null) {
            throw new DormException(DormExtCommands.WARNING_NOT_FOUND, "预警不存在");
        }
        return current;
    }

    // ================= 外来人员登记 =================

    /**
     * 学生提交来访登记。
     *
     * <p>房间不由客户端提供，而是按提交人的在住记录解析：否则学生可以把来访人
     * 登记到任意房间号上，宿管审核时也无从发现。</p>
     */
    public VisitorRegistrationDto submitVisitor(final SessionContext session,
                                                final VisitorRegistrationRequest request) {
        require(session, Permission.DORM_REQUEST);
        validateVisitor(request);
        return execute(new Work<VisitorRegistrationDto>() {
            @Override
            public VisitorRegistrationDto run(Connection c) throws Exception {
                Long roomId = repository.activeRoomOf(c, session.getUserId());
                if (roomId == null) {
                    throw new DormException(DormExtCommands.NO_ACCOMMODATION,
                            "没有有效的住宿记录，无法登记来访人员");
                }
                return repository.createVisitor(c, session.getUserId(), roomId.longValue(), request);
            }
        });
    }

    public DormPage<VisitorRegistrationDto> ownVisitors(final SessionContext session,
                                                        final DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<VisitorRegistrationDto>>() {
            @Override
            public DormPage<VisitorRegistrationDto> run(Connection c) throws Exception {
                return repository.listVisitors(c, q, Long.valueOf(session.getUserId()));
            }
        });
    }

    public DormPage<VisitorRegistrationDto> visitors(final SessionContext session,
                                                     final DormPageQuery query) {
        require(session, Permission.DORM_APPROVE);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<VisitorRegistrationDto>>() {
            @Override
            public DormPage<VisitorRegistrationDto> run(Connection c) throws Exception {
                return repository.listVisitors(c, q, null);
            }
        });
    }

    /** 学生撤销本人尚未审核的登记。 */
    public VisitorRegistrationDto cancelVisitor(final SessionContext session,
                                                final VisitorAuditRequest request) {
        require(session, Permission.DORM_REQUEST);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "撤销参数不能为空");
        }
        requireId(request.getRegistrationId(), "登记编号");
        return execute(new Work<VisitorRegistrationDto>() {
            @Override
            public VisitorRegistrationDto run(Connection c) throws Exception {
                VisitorRegistrationDto current = requireVisitor(c, request.getRegistrationId());
                if (current.getStudentUserId() != session.getUserId()) {
                    throw new DormException(ResultCodes.FORBIDDEN, "只能撤销本人提交的登记");
                }
                if (!current.isPending()) {
                    throw new DormException(DormExtCommands.VISITOR_INVALID_STATE,
                            "只有待审核的登记可以撤销");
                }
                return repository.updateVisitorStatus(c, request.getRegistrationId(),
                        VisitorRegistrationDto.STATUS_CANCELLED, null, null, request.getRemark());
            }
        });
    }

    public VisitorRegistrationDto auditVisitor(final SessionContext session,
                                               final VisitorAuditRequest request) {
        require(session, Permission.DORM_APPROVE);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "审核参数不能为空");
        }
        requireId(request.getRegistrationId(), "登记编号");
        return execute(new Work<VisitorRegistrationDto>() {
            @Override
            public VisitorRegistrationDto run(Connection c) throws Exception {
                VisitorRegistrationDto current = requireVisitor(c, request.getRegistrationId());
                if (!current.isPending()) {
                    throw new DormException(DormExtCommands.VISITOR_INVALID_STATE,
                            "该登记已处理，不能重复审核");
                }
                String status = request.isApproved()
                        ? VisitorRegistrationDto.STATUS_APPROVED
                        : VisitorRegistrationDto.STATUS_REJECTED;
                return repository.updateVisitorStatus(c, request.getRegistrationId(), status,
                        Long.valueOf(session.getUserId()), LocalDateTime.now(), request.getRemark());
            }
        });
    }

    private VisitorRegistrationDto requireVisitor(Connection c, long id) throws Exception {
        VisitorRegistrationDto current = repository.findVisitor(c, id);
        if (current == null) {
            throw new DormException(DormExtCommands.VISITOR_NOT_FOUND, "来访登记不存在");
        }
        return current;
    }

    private static void validateVisitor(VisitorRegistrationRequest request) {
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "登记参数不能为空");
        }
        requireText(request.getVisitorName(), "来访人姓名");
        requireText(request.getVisitorIdCard(), "来访人证件号");
        requireText(request.getVisitReason(), "来访事由");
        if (request.getStartAt() == null || request.getEndAt() == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "来访起止时间不能为空");
        }
        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "离开时间必须晚于来访时间");
        }
    }

    // ================= 卫生分项与检查任务 =================

    /**
     * 提交一次卫生检查。
     *
     * <p>客户端只报五项分数，总分、等级、是否整改全部由服务端算：让客户端算总分
     * 意味着它可以报一个和分项对不上的数字，而整改与复查都挂在总分上。</p>
     *
     * <p>不合格时在同一个事务里顺带建复查任务，对应设计文档的 rectifyNoticeSend
     * 与 recheckTaskCreate；同时把该房间到期的待检查任务标记完成。</p>
     */
    public HygieneDetailDto submitHygiene(final SessionContext session,
                                          final HygieneScoreSubmitRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "卫生检查参数不能为空");
        }
        requireId(request.getRoomId(), "房间编号");
        try {
            DormHygieneRules.validate(request.getItems());
        } catch (IllegalArgumentException ex) {
            throw new DormException(DormExtCommands.HYGIENE_ITEMS_INVALID, ex.getMessage());
        }
        return execute(new Work<HygieneDetailDto>() {
            @Override
            public HygieneDetailDto run(Connection c) throws Exception {
                LocalDateTime now = LocalDateTime.now();
                BigDecimal total = DormHygieneRules.total(request.getItems());
                long inspectionId = repository.createInspection(c, request.getRoomId(),
                        session.getUserId(), now, total, DormHygieneRules.result(total),
                        DormHygieneRules.status(total), request.getIssueDescription());
                repository.saveItemScores(c, inspectionId, request.getItems());
                repository.markTasksDone(c, request.getRoomId(), now.toLocalDate(), inspectionId);
                if (DormHygieneRules.needRectify(total)) {
                    repository.createTaskIfAbsent(c, request.getRoomId(),
                            HygieneTaskDto.TYPE_RECHECK,
                            DormHygieneRules.recheckDate(now.toLocalDate()),
                            Long.valueOf(inspectionId));
                }
                return repository.findInspectionDetail(c, inspectionId);
            }
        });
    }

    public HygieneDetailDto hygieneDetail(final SessionContext session,
                                          final HygieneDetailRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "查询参数不能为空");
        }
        requireId(request.getInspectionId(), "检查编号");
        return execute(new Work<HygieneDetailDto>() {
            @Override
            public HygieneDetailDto run(Connection c) throws Exception {
                HygieneDetailDto value = repository.findInspectionDetail(c,
                        request.getInspectionId());
                if (value == null) {
                    throw new DormException(DormExtCommands.HYGIENE_NOT_FOUND, "卫生检查记录不存在");
                }
                return value;
            }
        });
    }

    /** 按楼栋生成周检查清单；重复生成不会产生重复任务。 */
    public HygieneTaskGenerateResultDto generateHygieneTasks(final SessionContext session,
                                                             final HygieneTaskGenerateRequest request) {
        require(session, Permission.DORM_GOVERN);
        return execute(new Work<HygieneTaskGenerateResultDto>() {
            @Override
            public HygieneTaskGenerateResultDto run(Connection c) throws Exception {
                return hygiene(c, request);
            }
        });
    }

    private HygieneTaskGenerateResultDto hygiene(Connection c, HygieneTaskGenerateRequest request)
            throws Exception {
        LocalDate planDate = request == null || request.getPlanDate() == null
                ? LocalDate.now() : request.getPlanDate();
        Long buildingId = request == null ? null : request.getBuildingId();
        List<Long> rooms = repository.roomsForWeeklyTask(c, buildingId);
        int created = 0;
        for (Long roomId : rooms) {
            if (repository.createTaskIfAbsent(c, roomId.longValue(),
                    HygieneTaskDto.TYPE_WEEKLY, planDate, null)) {
                created++;
            }
        }
        return new HygieneTaskGenerateResultDto(planDate, rooms.size(), created,
                rooms.size() - created);
    }

    public DormPage<HygieneTaskDto> hygieneTasks(final SessionContext session,
                                                 final DormPageQuery query) {
        require(session, Permission.DORM_GOVERN);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<HygieneTaskDto>>() {
            @Override
            public DormPage<HygieneTaskDto> run(Connection c) throws Exception {
                return repository.listTasks(c, q);
            }
        });
    }

    // ================= 在宿状态、门禁、入内许可、房间删除 =================

    /** 学生查看本人在宿状态。 */
    public StayStatusDto myStayStatus(final SessionContext session) {
        require(session, Permission.DORM_SELF_READ);
        return execute(new Work<StayStatusDto>() {
            @Override
            public StayStatusDto run(Connection c) throws Exception {
                List<StayStatusDto> rows = resolveStay(c,
                        Long.valueOf(session.getUserId()), LocalDate.now());
                if (rows.isEmpty()) {
                    throw new DormException(DormExtCommands.NO_ACCOMMODATION, "没有有效的住宿记录");
                }
                return rows.get(0);
            }
        });
    }

    /** 宿管查看全部在住学生的在宿状态。 */
    public DormPage<StayStatusDto> stayStatuses(final SessionContext session) {
        require(session, Permission.DORM_GOVERN);
        return execute(new Work<DormPage<StayStatusDto>>() {
            @Override
            public DormPage<StayStatusDto> run(Connection c) throws Exception {
                List<StayStatusDto> rows = resolveStay(c, null, LocalDate.now());
                return new DormPage<StayStatusDto>(1, Math.max(rows.size(), 1), rows.size(), rows);
            }
        });
    }

    /** 把仓储给的原始行按请假与门禁流水判定成最终状态。 */
    private List<StayStatusDto> resolveStay(Connection c, Long studentUserId, LocalDate today)
            throws Exception {
        List<StayStatusDto> rows = repository.stayStatusRows(c, studentUserId);
        List<StayStatusDto> resolved = new ArrayList<StayStatusDto>(rows.size());
        for (StayStatusDto row : rows) {
            boolean onLeave = repository.hasApprovedLeave(c, row.getStudentUserId(), today);
            resolved.add(new StayStatusDto(row.getStudentUserId(), row.getRoomId(),
                    row.getBuildingCode(), row.getRoomNo(),
                    DormStayRules.status(row.getLastExitAt(), row.getLastEntryAt(), onLeave),
                    row.getLastExitAt(), row.getLastEntryAt()));
        }
        return resolved;
    }

    public DormPage<AccessRecordExtDto> myAccessRecords(final SessionContext session,
                                                        final DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<AccessRecordExtDto>>() {
            @Override
            public DormPage<AccessRecordExtDto> run(Connection c) throws Exception {
                return repository.listAccessRecords(c, session.getUserId(), q,
                        repository.loadAccessPolicy(c));
            }
        });
    }

    public AccessPolicyDto accessPolicy(final SessionContext session) {
        require(session, Permission.DORM_GOVERN);
        return execute(new Work<AccessPolicyDto>() {
            @Override
            public AccessPolicyDto run(Connection c) throws Exception {
                return repository.loadAccessPolicy(c);
            }
        });
    }

    public AccessPolicyDto saveAccessPolicy(final SessionContext session,
                                            final AccessPolicyRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null || request.getCurfewTime() == null || request.getDawnTime() == null) {
            throw new DormException(DormExtCommands.POLICY_INVALID, "门禁时间和清晨时间都不能为空");
        }
        if (!request.getDawnTime().isBefore(request.getCurfewTime())) {
            throw new DormException(DormExtCommands.POLICY_INVALID,
                    "清晨时间必须早于门禁时间，否则整天都会被判成晚归");
        }
        return execute(new Work<AccessPolicyDto>() {
            @Override
            public AccessPolicyDto run(Connection c) throws Exception {
                return repository.saveAccessPolicy(c, request, session.getUserId());
            }
        });
    }

    /** 学生对本人的报修单授权维修人员不在场入内。 */
    public RepairEntryPermitDto setRepairPermit(final SessionContext session,
                                                final RepairEntryPermitRequest request) {
        require(session, Permission.DORM_REQUEST);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "授权参数不能为空");
        }
        requireId(request.getRepairOrderId(), "工单号");
        return execute(new Work<RepairEntryPermitDto>() {
            @Override
            public RepairEntryPermitDto run(Connection c) throws Exception {
                Long reporter = repository.repairReporterOf(c, request.getRepairOrderId());
                if (reporter == null) {
                    throw new DormException(DormExtCommands.REPAIR_NOT_FOUND, "报修单不存在");
                }
                if (reporter.longValue() != session.getUserId()) {
                    throw new DormException(ResultCodes.FORBIDDEN, "只能为本人提交的报修单授权");
                }
                // 允许维修人员在本人不在场时进门，就必须留下能当场联系上的方式。
                // 优先用账号里登记的手机号（改了资料这里自动跟着变），没登记才要求填备注。
                if (request.isAllowEnter() && blank(repository.reporterPhoneOf(c,
                        request.getRepairOrderId())) && blank(request.getNote())) {
                    throw new DormException(DormExtCommands.CONTACT_REQUIRED,
                            "账号里没有登记手机号，请在备注里留下维修期间能联系到你的方式");
                }
                return repository.saveRepairPermit(c, request, session.getUserId());
            }
        });
    }

    public DormPage<RepairEntryPermitDto> myRepairPermits(final SessionContext session,
                                                          final DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<RepairEntryPermitDto>>() {
            @Override
            public DormPage<RepairEntryPermitDto> run(Connection c) throws Exception {
                return repository.listRepairPermits(c, session.getUserId(), q);
            }
        });
    }

    /**
     * 删除空置房间。
     *
     * <p>设计文档只要求「房间无在住学生方可删除」，这里再加一道历史引用检查：
     * 房间被账单、检查、工单等引用过就不删。既因为外键本来就会挡住，也因为
     * 删掉等于把这些历史记录的归属抹掉，出问题时无从追溯。</p>
     */
    public Long deleteRoom(final SessionContext session, final RoomDeleteRequest request) {
        require(session, Permission.DORM_MANAGE);
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "删除参数不能为空");
        }
        requireId(request.getRoomId(), "房间编号");
        return execute(new Work<Long>() {
            @Override
            public Long run(Connection c) throws Exception {
                if (repository.activeResidentCount(c, request.getRoomId()) > 0) {
                    throw new DormException(DormExtCommands.ROOM_OCCUPIED,
                            "房间仍有在住学生，不能删除");
                }
                if (repository.roomReferenceCount(c, request.getRoomId()) > 0) {
                    throw new DormException(DormExtCommands.ROOM_HAS_HISTORY,
                            "房间已有历史业务数据，不能删除；如需停用请把房间状态改为已关闭");
                }
                repository.deleteRoom(c, request.getRoomId());
                return Long.valueOf(request.getRoomId());
            }
        });
    }

    // ================= 公告类型、范围与置顶 =================

    /** 学生查看面向自己的宿舍公告：已发布、在有效期内、范围命中本人房间或楼栋。 */
    public DormPage<NoticeExtraDto> myNotices(final SessionContext session,
                                              final DormPageQuery query) {
        require(session, Permission.DORM_SELF_READ);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<NoticeExtraDto>>() {
            @Override
            public DormPage<NoticeExtraDto> run(Connection c) throws Exception {
                // 没有住宿记录（如刚入学未分配床位）时只看得到全体公告，而不是报错。
                Long room = repository.activeRoomOf(c, session.getUserId());
                return repository.listNotices(c, q, false, room);
            }
        });
    }

    /** 宿管查看全部宿舍公告（含草稿）及其扩展属性。 */
    public DormPage<NoticeExtraDto> notices(final SessionContext session,
                                            final DormPageQuery query) {
        require(session, Permission.DORM_GOVERN);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        requirePage(q);
        return execute(new Work<DormPage<NoticeExtraDto>>() {
            @Override
            public DormPage<NoticeExtraDto> run(Connection c) throws Exception {
                return repository.listNotices(c, q, true, null);
            }
        });
    }

    /**
     * 设置公告的类型、可见范围与置顶。
     *
     * <p>只写旁挂表，不触碰公告正文——标题、内容和发布状态仍由既有的
     * {@code dorm.announcement.save} 负责，两条路径互不干扰。</p>
     */
    public NoticeExtraDto saveNoticeExtra(final SessionContext session,
                                          final NoticeExtraRequest request) {
        require(session, Permission.DORM_GOVERN);
        validateNotice(request);
        return execute(new Work<NoticeExtraDto>() {
            @Override
            public NoticeExtraDto run(Connection c) throws Exception {
                if (repository.findNotice(c, request.getAnnouncementId()) == null) {
                    throw new DormException(DormExtCommands.NOTICE_NOT_FOUND,
                            "宿舍公告不存在");
                }
                return repository.saveNoticeExtra(c, request, session.getUserId());
            }
        });
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void validateNotice(NoticeExtraRequest request) {
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "公告设置参数不能为空");
        }
        requireId(request.getAnnouncementId(), "公告编号");
        if (!isKnownType(request.getNoticeType())) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "公告类型不正确");
        }
        String scope = request.getScopeType();
        // 三种范围各自的必填项在这里定死，与 V3 里的 CHECK 约束保持一致，
        // 让非法组合在服务层就被拒绝，而不是等数据库抛约束错误。
        if (NoticeExtraDto.SCOPE_ALL.equals(scope)) {
            if (request.getScopeBuildingId() != null || request.getScopeRoomId() != null) {
                throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID,
                        "面向全体的公告不能指定楼栋或房间");
            }
            return;
        }
        if (NoticeExtraDto.SCOPE_BUILDING.equals(scope)) {
            if (request.getScopeBuildingId() == null) {
                throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID,
                        "按楼栋投放时必须指定楼栋");
            }
            if (request.getScopeRoomId() != null) {
                throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID,
                        "按楼栋投放时不能同时指定房间");
            }
            requireId(request.getScopeBuildingId().longValue(), "楼栋编号");
            return;
        }
        if (NoticeExtraDto.SCOPE_ROOM.equals(scope)) {
            if (request.getScopeRoomId() == null) {
                throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID,
                        "按房间投放时必须指定房间");
            }
            requireId(request.getScopeRoomId().longValue(), "房间编号");
            return;
        }
        throw new DormException(DormExtCommands.NOTICE_SCOPE_INVALID, "公告可见范围不正确");
    }

    private static boolean isKnownType(String value) {
        return NoticeExtraDto.TYPE_GENERAL.equals(value)
                || NoticeExtraDto.TYPE_MAINTENANCE.equals(value)
                || NoticeExtraDto.TYPE_HYGIENE.equals(value)
                || NoticeExtraDto.TYPE_SAFETY.equals(value)
                || NoticeExtraDto.TYPE_URGENT.equals(value);
    }

    // ---- 输入校验 ----
    //
    // 不直接用基类的 id/text/page：它们抛的是 DORM.INVALID_INPUT，而本模块其余
    // 分支抛 DORM_EXT.INVALID_INPUT，同一类错误出现两种结果码会让客户端按码分支
    // 时踩空。这里统一成本模块的码，基类那三个保持原样供既有服务使用。

    private static void requireId(long value, String field) {
        if (value <= 0L) {
            throw new DormException(DormExtCommands.INVALID_INPUT, field + "不正确");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new DormException(DormExtCommands.INVALID_INPUT, field + "不能为空");
        }
    }

    private static void requirePage(DormPageQuery query) {
        if (query.getPage() <= 0 || query.getPageSize() <= 0 || query.getPageSize() > 100) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "分页参数不正确");
        }
    }

    /**
     * 服务端运行状态：模块版本、调度器是否在跑、每个定时任务的最近一次结果。
     *
     * <p>不访问数据库，因此即使数据库暂时不可用也能用它确认命令路由与鉴权是通的。</p>
     */
    public DormExtStatusDto status(SessionContext session) {
        require(session, Permission.DORM_GOVERN);
        return new DormExtStatusDto(MODULE_VERSION, schedulerStatus.isRunning(),
                schedulerStatus.describe(), LocalDateTime.now());
    }

    private static void validate(MeterReadingRequest request) {
        if (request == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "抄表参数不能为空");
        }
        requireId(request.getRoomId(), "房间编号");
        if (request.getPeriodStart() == null || request.getPeriodEnd() == null) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "账期起止日期不能为空");
        }
        if (request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "账期结束日期不能早于开始日期");
        }
        atLeastZero(request.getElectricityUnits(), "用电量");
        atLeastZero(request.getWaterUnits(), "用水量");
        aboveZero(request.getElectricityPrice(), "电费单价");
        aboveZero(request.getWaterPrice(), "水费单价");
    }

    private static void atLeastZero(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new DormException(DormExtCommands.INVALID_INPUT, field + "不能为负数");
        }
    }

    private static void aboveZero(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new DormException(DormExtCommands.INVALID_INPUT, field + "必须大于零");
        }
    }

    // ================= 定时任务入口 =================
    //
    // 调度线程没有 SessionContext，也就没有可校验的权限主体。下面这些方法只在
    // 服务端进程内部由 DormScheduler 调用，既不注册命令也不经过 CommandRouter，
    // 客户端无从触达，因此不做 require(...)；它们复用与人工入口完全相同的编排
    // 代码（scan/hygiene/generate），保证定时结果与手动触发结果一致。

    /** 定时任务台账；调度器启动后往里写运行结果。 */
    public DormSchedulerStatus schedulerStatus() { return schedulerStatus; }

    /** 服务端启动时由 DormScheduler 调用，把「按需触发」的能力挂上来。 */
    public void attachTaskRunner(DormTaskRunner runner) { this.taskRunner = runner; }

    /**
     * 立刻执行一个定时任务并返回刷新后的运行状态。
     *
     * <p>定时任务最早的一个也要等到次日 08:00，靠等没法确认接线是通的。这个入口让
     * 宿管在监控页面上点一下就能把任务跑一遍，跑完的结果落进同一份台账，和到点自动
     * 执行走的是完全相同的代码路径。</p>
     */
    public DormExtStatusDto runScheduledTask(SessionContext session, String taskName) {
        require(session, Permission.DORM_GOVERN);
        DormTaskRunner runner = taskRunner;
        if (runner == null) {
            throw new DormException(DormExtCommands.SCHEDULER_UNAVAILABLE,
                    "定时任务调度器未启动，无法手动触发");
        }
        runner.runNow(taskName == null || taskName.trim().isEmpty() ? null : taskName.trim());
        return status(session);
    }

    /** 定时未归扫描；与宿管手动扫描共用同一套阈值与去重逻辑。 */
    public WarningScanResultDto runAbsenceScan(final LocalDate scanDate) {
        return execute(new Work<WarningScanResultDto>() {
            @Override
            public WarningScanResultDto run(Connection c) throws Exception {
                return scan(c, scanDate);
            }
        });
    }

    /** 定时生成周卫生检查任务；重复生成不会产生重复任务。 */
    public HygieneTaskGenerateResultDto runHygieneTaskGenerate(
            final HygieneTaskGenerateRequest request) {
        return execute(new Work<HygieneTaskGenerateResultDto>() {
            @Override
            public HygieneTaskGenerateResultDto run(Connection c) throws Exception {
                return hygiene(c, request);
            }
        });
    }

    /** 定时出账；operatorUserId 是记账人，必须是真实存在的用户号。 */
    public BillGenerateResultDto runBillGenerate(final BillGenerateRequest request,
                                                 final long operatorUserId) {
        validatePeriod(request);
        requireId(operatorUserId, "记账人用户号");
        return execute(new Work<BillGenerateResultDto>() {
            @Override
            public BillGenerateResultDto run(Connection c) throws Exception {
                return generate(c, request, operatorUserId);
            }
        });
    }

    /** 定时下架已过期的宿舍公告，返回下架条数。 */
    public int runNoticeExpireScan(final LocalDateTime now) {
        Integer affected = execute(new Work<Integer>() {
            @Override
            public Integer run(Connection c) throws Exception {
                return Integer.valueOf(repository.expireDormAnnouncements(c, now));
            }
        });
        return affected == null ? 0 : affected.intValue();
    }

    /**
     * 仍未处理的严重未归预警。
     *
     * <p>设计文档要求「通知辅导员」，但 main 的 {@code student_profiles} 里没有
     * 学生到辅导员的映射，服务端无从自动定位收件人。因此定时任务只汇总待处理的
     * 严重预警并写入运行日志与监控台账，真正的通知动作仍走宿管手动的
     * {@code dorm.ext.warning.notify}（要求填写辅导员用户号）。</p>
     */
    public List<AbsenceWarningDto> pendingSevereWarnings(final LocalDate onOrBefore) {
        return execute(new Work<List<AbsenceWarningDto>>() {
            @Override
            public List<AbsenceWarningDto> run(Connection c) throws Exception {
                return repository.pendingSevereWarnings(c, onOrBefore);
            }
        });
    }
}
