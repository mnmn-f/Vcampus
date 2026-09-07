package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** Thread-safe in-memory facade; domain stores keep each persistence concern small. */
public final class InMemoryDormExtRepository implements DormExtRepository {
    private final InMemoryDormExtState state = new InMemoryDormExtState();
    private final InMemoryDormMeterStore meter = new InMemoryDormMeterStore(state);
    private final InMemoryDormWarningStore warning = new InMemoryDormWarningStore(state);
    private final InMemoryDormVisitorStore visitor = new InMemoryDormVisitorStore(state);
    private final InMemoryDormHygieneStore hygiene = new InMemoryDormHygieneStore(state);
    private final InMemoryDormAccessStore access = new InMemoryDormAccessStore(state);
    private final InMemoryDormNoticeStore notice = new InMemoryDormNoticeStore(state);

    public synchronized void addRoom(long id, String building, String roomNo) { meter.addRoom(id, building, roomNo); }
    public synchronized void setResidents(long roomId, Long... ids) { meter.setResidents(roomId, ids); }
    public synchronized void attachBill(long readingId, long billId) { meter.attachBill(readingId, billId); }
    public synchronized List<BigDecimal> allocationsOf(long billId) { return meter.allocationsOf(billId); }
    public synchronized int billCount() { return meter.billCount(); }
    public synchronized void addResident(long id, long roomId, LocalDateTime exit, LocalDateTime entry) { warning.addResident(id, roomId, exit, entry); }
    public synchronized void addApprovedLeave(long id, LocalDate date) { warning.addApprovedLeave(id, date); }
    public synchronized void addAccessRecord(long id, long student, String type, LocalDateTime at, String door) { access.addAccess(id, student, type, at, door); }
    public synchronized void addRepairOrder(long order, long reporter, long roomId, String category, String status) { access.addRepair(order, reporter, roomId, category, status); }
    public synchronized void setRepairPriority(long order, String priority) { access.setPriority(order, priority); }
    public synchronized void addRepairWorker(long userId, String displayName) { access.addRepairWorker(userId, displayName); }
    public synchronized void setPhone(long id, String phone) { access.setPhone(id, phone); }
    public synchronized void addAccommodation(long student, long roomId) { visitor.addAccommodation(student, roomId); }
    public synchronized void addTask(long id, long roomId, String type, LocalDate date, String status) { hygiene.addTask(id, roomId, type, date, status); }
    public synchronized void addBuilding(long id, String code) { notice.addBuilding(id, code); }
    public synchronized void setRoomBuilding(long roomId, long buildingId) { notice.setRoomBuilding(roomId, buildingId); }
    public synchronized void addNotice(long id, String title, String status) { notice.addNotice(id, title, status); }
    public synchronized void setExpirableAnnouncements(int count) { notice.setExpirable(count); }

    @Override public synchronized DormPage<MeterReadingDto> listMeterReadings(Connection c, DormPageQuery q) { return meter.list(q); }
    @Override public synchronized MeterReadingDto saveMeterReading(Connection c, MeterReadingRequest r, long actor) { return meter.save(r, actor); }
    @Override public synchronized List<MeterReadingDto> pendingReadings(Connection c, Long room, LocalDate start, LocalDate end) { return meter.pending(room, start, end); }
    @Override public synchronized List<Long> activeResidents(Connection c, long room) { return meter.residents(room); }
    @Override public synchronized boolean billExists(Connection c, long room, LocalDate start, LocalDate end) { return meter.billExists(room, start, end); }
    @Override public synchronized long createBill(Connection c, MeterReadingDto reading, BigDecimal amount, LocalDateTime due, long actor) { return meter.createBill(reading); }
    @Override public synchronized void createAllocation(Connection c, long bill, long student, BigDecimal amount) { meter.allocation(bill, amount); }
    @Override public synchronized void linkReadingToBill(Connection c, long reading, long bill) { meter.attachBill(reading, bill); }

    @Override public synchronized List<ResidentAbsenceSnapshot> residentsForScan(Connection c) { return warning.residentsForScan(); }
    @Override public synchronized boolean hasApprovedLeave(Connection c, long id, LocalDate date) { return warning.hasApprovedLeave(id, date); }
    @Override public synchronized AbsenceWarningDto saveWarning(Connection c, long student, long room, LocalDate date, LocalDateTime last, int days, String level) { return warning.save(student, room, date, last, days, level); }
    @Override public synchronized DormPage<AbsenceWarningDto> listWarnings(Connection c, DormPageQuery q) { return warning.list(q); }
    @Override public synchronized AbsenceWarningDto findWarning(Connection c, long id) { return warning.find(id); }
    @Override public synchronized AbsenceWarningDto updateWarningStatus(Connection c, long id, String status, Long teacher, LocalDateTime at, String note) { return warning.update(id, status, teacher, at, note); }
    @Override public synchronized WarningConfigDto loadWarningConfig(Connection c) { return warning.config(); }
    @Override public synchronized WarningConfigDto saveWarningConfig(Connection c, WarningConfigRequest r, long actor) { return warning.saveConfig(r); }

    @Override public synchronized Long activeRoomOf(Connection c, long student) { return visitor.activeRoom(student); }
    @Override public synchronized VisitorRegistrationDto createVisitor(Connection c, long student, long room, VisitorRegistrationRequest r) { return visitor.create(student, room, r); }
    @Override public synchronized DormPage<VisitorRegistrationDto> listVisitors(Connection c, DormPageQuery q, Long student) { return visitor.list(q, student); }
    @Override public synchronized VisitorRegistrationDto findVisitor(Connection c, long id) { return visitor.find(id); }
    @Override public synchronized VisitorRegistrationDto updateVisitorStatus(Connection c, long id, String status, Long auditor, LocalDateTime at, String remark) { return visitor.update(id, status, auditor, at, remark); }

    @Override public synchronized long createInspection(Connection c, long room, long inspector, LocalDateTime at, BigDecimal total, String result, String status, String issue) { return hygiene.createInspection(room, inspector, at, total, issue); }
    @Override public synchronized void saveItemScores(Connection c, long id, List<HygieneItemScoreDto> items) { hygiene.saveItemScores(id, items); }
    @Override public synchronized HygieneDetailDto findInspectionDetail(Connection c, long id) { return hygiene.detail(id); }
    @Override public synchronized List<Long> roomsForWeeklyTask(Connection c, Long building) { return hygiene.rooms(building); }
    @Override public synchronized boolean createTaskIfAbsent(Connection c, long room, String type, LocalDate date, Long source) { return hygiene.createTaskIfAbsent(room, type, date, source); }
    @Override public synchronized int markTasksDone(Connection c, long room, LocalDate before, long inspection) { return hygiene.markTasksDone(room, before, inspection); }
    @Override public synchronized DormPage<HygieneTaskDto> listTasks(Connection c, DormPageQuery q) { return hygiene.listTasks(q); }

    /**
     * 内存实现只回填住宿与同寝人数，账单和卫生留空。
     *
     * <p>首屏摘要要读的账单、卫生检查在内存实现里根本没有对应的存储——它们只在
     * MySQL 那套里有表。与其为了这一个方法凭空造两套假数据，不如老实返回空值：
     * 用到它的测试关心的是「有没有在住记录、同寝几个人」，那两格显示占位就对了。</p>
     */
    @Override public synchronized DormHomeSummaryDto homeSummary(Connection c, long student) {
        Long room = visitor.activeRoom(student);
        if (room == null) return DormHomeSummaryDto.empty();
        int occupied = access.activeResidents(room.longValue());
        return new DormHomeSummaryDto(true, room.longValue(), null, null, null, 0, occupied,
                java.math.BigDecimal.ZERO, 0, null, null, null, null, null, 0);
    }
    @Override public synchronized DormPage<RepairWorkOrderDto> repairQueue(Connection c, DormPageQuery q) { return access.repairQueue(q); }
    @Override public synchronized DormPage<RepairWorkOrderDto> repairAssigned(Connection c, long handler, DormPageQuery q, boolean active) { return access.repairAssigned(handler, q, active); }
    @Override public synchronized RepairWorkOrderDto findRepairWork(Connection c, long order, long handler) { return access.findRepairWork(order, handler); }
    @Override public synchronized int claimRepair(Connection c, long order, long handler) { return access.claimRepair(order, handler); }
    @Override public synchronized int startRepair(Connection c, long order, long handler) { return access.startRepair(order, handler); }
    @Override public synchronized int finishRepair(Connection c, long order, long handler) { return access.finishRepair(order, handler); }
    @Override public synchronized List<RepairWorkerDto> repairWorkers(Connection c) { return access.repairWorkers(); }
    @Override public synchronized int assignRepair(Connection c, long order, long worker) { return access.assignRepair(order, worker); }
    @Override public synchronized int reviewRepair(Connection c, long order, boolean approved) { return access.reviewRepair(order, approved); }
    @Override public synchronized RepairWorkOrderDto findRepairForManager(Connection c, long order) { return access.findRepairForManager(order); }
    @Override public synchronized List<StayStatusDto> stayStatusRows(Connection c, Long student) { return access.stayRows(student); }
    @Override public synchronized AccessPolicyDto loadAccessPolicy(Connection c) { return access.policy(); }
    @Override public synchronized AccessPolicyDto saveAccessPolicy(Connection c, AccessPolicyRequest r, long actor) { return access.savePolicy(r); }
    @Override public synchronized DormPage<AccessRecordExtDto> listAccessRecords(Connection c, long student, DormPageQuery q, AccessPolicyDto policy) { return access.access(student, q, policy); }
    @Override public synchronized Long repairReporterOf(Connection c, long order) { return access.reporter(order); }
    @Override public synchronized String reporterPhoneOf(Connection c, long order) { return access.reporterPhone(order); }
    @Override public synchronized RepairEntryPermitDto saveRepairPermit(Connection c, RepairEntryPermitRequest r, long actor) { return access.savePermit(r); }
    @Override public synchronized DormPage<RepairEntryPermitDto> listRepairPermits(Connection c, long student, DormPageQuery q) { return access.permits(student, q); }
    @Override public synchronized int activeResidentCount(Connection c, long room) { return access.activeResidents(room); }
    @Override public synchronized int roomReferenceCount(Connection c, long room) { return references(room); }
    @Override public synchronized void deleteRoom(Connection c, long room) {
        if (state.rooms.remove(Long.valueOf(room)) == null) throw new DormRepositoryException(edu.seu.vcampus.common.protocol.command.DormExtCommands.ROOM_NOT_FOUND, "房间不存在");
    }

    @Override public synchronized DormPage<NoticeExtraDto> listNotices(Connection c, DormPageQuery q, boolean manage, Long room) { return notice.list(q, manage, room); }
    @Override public synchronized NoticeExtraDto findNotice(Connection c, long id) { return notice.find(id); }
    @Override public synchronized NoticeExtraDto saveNoticeExtra(Connection c, NoticeExtraRequest r, long actor) { return notice.save(r); }
    @Override public synchronized int expireDormAnnouncements(Connection c, LocalDateTime now) { return notice.expire(); }
    @Override public synchronized List<AbsenceWarningDto> pendingSevereWarnings(Connection c, LocalDate date) { return warning.pendingSevere(date); }

    private int references(long room) {
        int count = access.activeResidents(room);
        for (MeterReadingDto item : state.readings.values()) if (item.getRoomId() == room) count++;
        for (HygieneTaskDto item : state.tasks.values()) if (item.getRoomId() == room) count++;
        for (AbsenceWarningDto item : state.warnings.values()) if (item.getRoomId() == room) count++;
        for (VisitorRegistrationDto item : state.visitors.values()) if (item.getRoomId() == room) count++;
        for (RepairEntryPermitDto item : state.repairOrders.values()) if (item.getRoomId() == room) count++;
        return count;
    }
}
