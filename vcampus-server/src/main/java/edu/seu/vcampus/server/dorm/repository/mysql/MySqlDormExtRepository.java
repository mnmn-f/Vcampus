package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.server.dorm.repository.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** Facade over the extension's small, function-oriented MySQL repositories. */
public final class MySqlDormExtRepository implements DormExtRepository {
    private final MySqlDormMeterRepository meter = new MySqlDormMeterRepository();
    private final MySqlDormWarningRepository warning = new MySqlDormWarningRepository();
    private final MySqlDormVisitorRepository visitor = new MySqlDormVisitorRepository();
    private final MySqlDormExtHygieneRepository hygiene = new MySqlDormExtHygieneRepository();
    private final MySqlDormExtAccessRepository access = new MySqlDormExtAccessRepository();
    private final MySqlDormNoticeRepository notice = new MySqlDormNoticeRepository();
    private final MySqlDormHomeRepository home = new MySqlDormHomeRepository();
    private final MySqlDormRepairWorkRepository repairWork = new MySqlDormRepairWorkRepository();

    @Override public DormPage<MeterReadingDto> listMeterReadings(Connection c, DormPageQuery q) throws SQLException { return meter.list(c, q); }
    @Override public MeterReadingDto saveMeterReading(Connection c, MeterReadingRequest r, long actor) throws SQLException { return meter.save(c, r, actor); }
    @Override public List<MeterReadingDto> pendingReadings(Connection c, Long room, LocalDate start, LocalDate end) throws SQLException { return meter.pending(c, room, start, end); }
    @Override public List<Long> activeResidents(Connection c, long room) throws SQLException { return meter.activeResidents(c, room); }
    @Override public boolean billExists(Connection c, long room, LocalDate start, LocalDate end) throws SQLException { return meter.billExists(c, room, start, end); }
    @Override public long createBill(Connection c, MeterReadingDto reading, BigDecimal amount, LocalDateTime due, long actor) throws SQLException { return meter.createBill(c, reading, amount, due, actor); }
    @Override public void createAllocation(Connection c, long bill, long student, BigDecimal amount) throws SQLException { meter.createAllocation(c, bill, student, amount); }
    @Override public void linkReadingToBill(Connection c, long reading, long bill) throws SQLException { meter.link(c, reading, bill); }

    @Override public List<ResidentAbsenceSnapshot> residentsForScan(Connection c) throws SQLException { return warning.residents(c); }
    @Override public boolean hasApprovedLeave(Connection c, long student, LocalDate date) throws SQLException { return warning.hasApprovedLeave(c, student, date); }
    @Override public AbsenceWarningDto saveWarning(Connection c, long student, long room, LocalDate date, LocalDateTime last, int days, String level) throws SQLException { return warning.save(c, student, room, date, last, days, level); }
    @Override public DormPage<AbsenceWarningDto> listWarnings(Connection c, DormPageQuery q) throws SQLException { return warning.list(c, q); }
    @Override public AbsenceWarningDto findWarning(Connection c, long id) throws SQLException { return warning.find(c, id); }
    @Override public AbsenceWarningDto updateWarningStatus(Connection c, long id, String status, Long teacher, LocalDateTime at, String note) throws SQLException { return warning.update(c, id, status, teacher, at, note); }
    @Override public WarningConfigDto loadWarningConfig(Connection c) throws SQLException { return warning.config(c); }
    @Override public WarningConfigDto saveWarningConfig(Connection c, WarningConfigRequest r, long actor) throws SQLException { return warning.saveConfig(c, r, actor); }
    @Override public List<AbsenceWarningDto> pendingSevereWarnings(Connection c, LocalDate date) throws SQLException { return warning.pendingSevere(c, date); }

    @Override public Long activeRoomOf(Connection c, long student) throws SQLException { return visitor.activeRoom(c, student); }
    @Override public VisitorRegistrationDto createVisitor(Connection c, long student, long room, VisitorRegistrationRequest r) throws SQLException { return visitor.create(c, student, room, r); }
    @Override public DormPage<VisitorRegistrationDto> listVisitors(Connection c, DormPageQuery q, Long student) throws SQLException { return visitor.list(c, q, student); }
    @Override public VisitorRegistrationDto findVisitor(Connection c, long id) throws SQLException { return visitor.find(c, id); }
    @Override public VisitorRegistrationDto updateVisitorStatus(Connection c, long id, String status, Long auditor, LocalDateTime at, String remark) throws SQLException { return visitor.update(c, id, status, auditor, at, remark); }

    @Override public long createInspection(Connection c, long room, long inspector, LocalDateTime at, BigDecimal total, String result, String status, String issue) throws SQLException { return hygiene.createInspection(c, room, inspector, at, total, result, status, issue); }
    @Override public void saveItemScores(Connection c, long id, List<HygieneItemScoreDto> items) throws SQLException { hygiene.saveItemScores(c, id, items); }
    @Override public HygieneDetailDto findInspectionDetail(Connection c, long id) throws SQLException { return hygiene.detail(c, id); }
    @Override public List<Long> roomsForWeeklyTask(Connection c, Long building) throws SQLException { return hygiene.rooms(c, building); }
    @Override public boolean createTaskIfAbsent(Connection c, long room, String type, LocalDate date, Long source) throws SQLException { return hygiene.createTaskIfAbsent(c, room, type, date, source); }
    @Override public int markTasksDone(Connection c, long room, LocalDate before, long inspection) throws SQLException { return hygiene.markTasksDone(c, room, before, inspection); }
    @Override public DormPage<HygieneTaskDto> listTasks(Connection c, DormPageQuery q) throws SQLException { return hygiene.listTasks(c, q); }

    @Override public DormHomeSummaryDto homeSummary(Connection c, long student) throws SQLException { return home.summary(c, student); }
    @Override public DormPage<RepairWorkOrderDto> repairQueue(Connection c, DormPageQuery q) throws SQLException { return repairWork.queue(c, q); }
    @Override public DormPage<RepairWorkOrderDto> repairAssigned(Connection c, long handler, DormPageQuery q, boolean active) throws SQLException { return repairWork.assigned(c, handler, q, active); }
    @Override public RepairWorkOrderDto findRepairWork(Connection c, long order, long handler) throws SQLException { return repairWork.find(c, order, handler); }
    @Override public int claimRepair(Connection c, long order, long handler) throws SQLException { return repairWork.claim(c, order, handler); }
    @Override public int startRepair(Connection c, long order, long handler) throws SQLException { return repairWork.start(c, order, handler); }
    @Override public int finishRepair(Connection c, long order, long handler) throws SQLException { return repairWork.finish(c, order, handler); }
    @Override public List<RepairWorkerDto> repairWorkers(Connection c) throws SQLException { return repairWork.workers(c); }
    @Override public int assignRepair(Connection c, long order, long worker) throws SQLException { return repairWork.assign(c, order, worker); }
    @Override public int reviewRepair(Connection c, long order, boolean approved) throws SQLException { return repairWork.review(c, order, approved); }
    @Override public RepairWorkOrderDto findRepairForManager(Connection c, long order) throws SQLException { return repairWork.findForManager(c, order); }
    @Override public List<StayStatusDto> stayStatusRows(Connection c, Long student) throws SQLException { return access.stayRows(c, student); }
    @Override public AccessPolicyDto loadAccessPolicy(Connection c) throws SQLException { return access.policy(c); }
    @Override public AccessPolicyDto saveAccessPolicy(Connection c, AccessPolicyRequest r, long actor) throws SQLException { return access.savePolicy(c, r, actor); }
    @Override public DormPage<AccessRecordExtDto> listAccessRecords(Connection c, long student, DormPageQuery q, AccessPolicyDto policy) throws SQLException { return access.access(c, student, q, policy); }
    @Override public Long repairReporterOf(Connection c, long order) throws SQLException { return access.repairReporter(c, order); }
    @Override public String reporterPhoneOf(Connection c, long order) throws SQLException { return access.reporterPhone(c, order); }
    @Override public RepairEntryPermitDto saveRepairPermit(Connection c, RepairEntryPermitRequest r, long actor) throws SQLException { return access.savePermit(c, r, actor); }
    @Override public DormPage<RepairEntryPermitDto> listRepairPermits(Connection c, long student, DormPageQuery q) throws SQLException { return access.permits(c, student, q); }
    @Override public int activeResidentCount(Connection c, long room) throws SQLException { return access.activeResidentCount(c, room); }
    @Override public int roomReferenceCount(Connection c, long room) throws SQLException { return access.roomReferenceCount(c, room); }
    @Override public void deleteRoom(Connection c, long room) throws SQLException { access.deleteRoom(c, room); }

    @Override public DormPage<NoticeExtraDto> listNotices(Connection c, DormPageQuery q, boolean manage, Long room) throws SQLException { return notice.list(c, q, manage, room); }
    @Override public NoticeExtraDto findNotice(Connection c, long id) throws SQLException { return notice.find(c, id); }
    @Override public NoticeExtraDto saveNoticeExtra(Connection c, NoticeExtraRequest r, long actor) throws SQLException { return notice.save(c, r, actor); }
    @Override public int expireDormAnnouncements(Connection c, LocalDateTime now) throws SQLException { return notice.expire(c, now); }
}
