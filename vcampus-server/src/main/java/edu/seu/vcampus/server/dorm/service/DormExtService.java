package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import edu.seu.vcampus.server.security.SessionContext;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** Public facade for the dorm extension; domain workflows live in focused services. */
public final class DormExtService extends DormServiceSupport {
    public static final String MODULE_VERSION = "dorm-ext/P6";
    private final DormExtMeterService meter;
    private final DormExtWarningService warning;
    private final DormExtVisitorService visitor;
    private final DormExtHygieneService hygiene;
    private final DormExtAccessService access;
    private final DormExtNoticeService notice;
    private final DormExtRepairWorkService repairWork;
    private final DormSchedulerStatus schedulerStatus = new DormSchedulerStatus();
    private volatile DormTaskRunner taskRunner;

    public DormExtService(DormExtRepository repository, TransactionManager transactions) {
        super(transactions);
        if (repository == null) throw new IllegalArgumentException("repository is required");
        meter = new DormExtMeterService(repository, transactions); warning = new DormExtWarningService(repository, transactions);
        visitor = new DormExtVisitorService(repository, transactions); hygiene = new DormExtHygieneService(repository, transactions);
        access = new DormExtAccessService(repository, transactions); notice = new DormExtNoticeService(repository, transactions);
        repairWork = new DormExtRepairWorkService(repository, transactions);
    }
    public DormExtService(DormExtRepository repository) { this(repository, null); }

    public DormPage<MeterReadingDto> meterReadings(SessionContext s, DormPageQuery q) { return meter.list(s, q); }
    public MeterReadingDto saveMeterReading(SessionContext s, MeterReadingRequest r) { return meter.save(s, r); }
    public BillGenerateResultDto generateBills(SessionContext s, BillGenerateRequest r) { return meter.generateBills(s, r); }
    public WarningScanResultDto scanAbsences(SessionContext s, WarningScanRequest r) { return warning.scan(s, r); }
    public DormPage<AbsenceWarningDto> warnings(SessionContext s, DormPageQuery q) { return warning.list(s, q); }
    public AbsenceWarningDto notifyWarning(SessionContext s, WarningHandleRequest r) { return warning.notify(s, r); }
    public AbsenceWarningDto verifyWarning(SessionContext s, WarningHandleRequest r) { return warning.verify(s, r); }
    public WarningConfigDto warningConfig(SessionContext s) { return warning.config(s); }
    public WarningConfigDto saveWarningConfig(SessionContext s, WarningConfigRequest r) { return warning.saveConfig(s, r); }
    public VisitorRegistrationDto submitVisitor(SessionContext s, VisitorRegistrationRequest r) { return visitor.submit(s, r); }
    public DormPage<VisitorRegistrationDto> ownVisitors(SessionContext s, DormPageQuery q) { return visitor.own(s, q); }
    public DormPage<VisitorRegistrationDto> visitors(SessionContext s, DormPageQuery q) { return visitor.list(s, q); }
    public VisitorRegistrationDto cancelVisitor(SessionContext s, VisitorAuditRequest r) { return visitor.cancel(s, r); }
    public VisitorRegistrationDto auditVisitor(SessionContext s, VisitorAuditRequest r) { return visitor.audit(s, r); }
    public HygieneDetailDto submitHygiene(SessionContext s, HygieneScoreSubmitRequest r) { return hygiene.submit(s, r); }
    public HygieneDetailDto hygieneDetail(SessionContext s, HygieneDetailRequest r) { return hygiene.detail(s, r); }
    public HygieneTaskGenerateResultDto generateHygieneTasks(SessionContext s, HygieneTaskGenerateRequest r) { return hygiene.generateTasks(s, r); }
    public DormPage<HygieneTaskDto> hygieneTasks(SessionContext s, DormPageQuery q) { return hygiene.tasks(s, q); }
    public DormHomeSummaryDto homeSummary(SessionContext s) { return access.homeSummary(s); }
    public DormPage<RepairWorkOrderDto> repairQueue(SessionContext s, DormPageQuery q) { return repairWork.queue(s, q); }
    public DormPage<RepairWorkOrderDto> repairAssigned(SessionContext s, DormPageQuery q) { return repairWork.assigned(s, q, true); }
    public DormPage<RepairWorkOrderDto> repairHistory(SessionContext s, DormPageQuery q) { return repairWork.assigned(s, q, false); }
    public java.util.List<RepairWorkerDto> repairWorkers(SessionContext s) { return repairWork.workers(s); }
    public RepairWorkOrderDto repairDetail(SessionContext s, Long orderId) { return repairWork.detail(s, orderId == null ? 0L : orderId.longValue()); }
    public RepairWorkOrderDto assignRepair(SessionContext s, RepairAssignRequest r) { return repairWork.assign(s, r); }
    public RepairWorkOrderDto reviewRepair(SessionContext s, RepairWorkRequest r, boolean approved) { return repairWork.review(s, r, approved); }
    public RepairWorkOrderDto acceptRepair(SessionContext s, RepairWorkRequest r) { return repairWork.accept(s, r); }
    public RepairWorkOrderDto startRepair(SessionContext s, RepairWorkRequest r) { return repairWork.start(s, r); }
    public RepairWorkOrderDto finishRepair(SessionContext s, RepairWorkRequest r) { return repairWork.finish(s, r); }
    public StayStatusDto myStayStatus(SessionContext s) { return access.myStay(s); }
    public DormPage<StayStatusDto> stayStatuses(SessionContext s) { return access.stays(s); }
    public DormPage<AccessRecordExtDto> myAccessRecords(SessionContext s, DormPageQuery q) { return access.access(s, q); }
    public AccessPolicyDto accessPolicy(SessionContext s) { return access.policy(s); }
    public AccessPolicyDto saveAccessPolicy(SessionContext s, AccessPolicyRequest r) { return access.savePolicy(s, r); }
    public RepairEntryPermitDto setRepairPermit(SessionContext s, RepairEntryPermitRequest r) { return access.setPermit(s, r); }
    public DormPage<RepairEntryPermitDto> myRepairPermits(SessionContext s, DormPageQuery q) { return access.permits(s, q); }
    public Long deleteRoom(SessionContext s, RoomDeleteRequest r) { return access.deleteRoom(s, r); }
    public DormPage<NoticeExtraDto> myNotices(SessionContext s, DormPageQuery q) { return notice.mine(s, q); }
    public DormPage<NoticeExtraDto> notices(SessionContext s, DormPageQuery q) { return notice.list(s, q); }
    public NoticeExtraDto saveNoticeExtra(SessionContext s, NoticeExtraRequest r) { return notice.save(s, r); }

    public DormExtStatusDto status(SessionContext session) {
        require(session, Permission.DORM_GOVERN);
        return new DormExtStatusDto(MODULE_VERSION, schedulerStatus.isRunning(), schedulerStatus.describe(), LocalDateTime.now());
    }
    public DormSchedulerStatus schedulerStatus() { return schedulerStatus; }
    public void attachTaskRunner(DormTaskRunner runner) { taskRunner = runner; }
    public DormExtStatusDto runScheduledTask(SessionContext session, String taskName) {
        require(session, Permission.DORM_GOVERN); DormTaskRunner runner = taskRunner;
        if (runner == null) throw new DormException(DormExtCommands.SCHEDULER_UNAVAILABLE, "定时任务调度器未启动，无法手动触发");
        runner.runNow(taskName == null || taskName.trim().isEmpty() ? null : taskName.trim()); return status(session);
    }
    public WarningScanResultDto runAbsenceScan(LocalDate date) { return warning.scanScheduled(date); }
    public HygieneTaskGenerateResultDto runHygieneTaskGenerate(HygieneTaskGenerateRequest r) { return hygiene.generateScheduled(r); }
    public BillGenerateResultDto runBillGenerate(BillGenerateRequest r, long operator) { return meter.generateScheduled(r, operator); }
    public int runNoticeExpireScan(LocalDateTime now) { return notice.expire(now); }
    public List<AbsenceWarningDto> pendingSevereWarnings(LocalDate date) { return warning.pendingSevere(date); }
}
