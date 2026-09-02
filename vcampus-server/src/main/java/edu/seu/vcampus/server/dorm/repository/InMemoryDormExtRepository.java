package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyDto;
import edu.seu.vcampus.common.dto.dorm.ext.AccessPolicyRequest;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairEntryPermitRequest;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneItemScoreDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;

import edu.seu.vcampus.server.dorm.service.DormHygieneRules;
import edu.seu.vcampus.server.dorm.service.DormStayRules;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/** 内存版扩展仓储；让服务层单测不依赖真实数据库。 */
public final class InMemoryDormExtRepository implements DormExtRepository {
    private final Map<Long, MeterReadingDto> readings = new LinkedHashMap<Long, MeterReadingDto>();
    private final Map<Long, String> rooms = new LinkedHashMap<Long, String>();
    private final Map<Long, List<Long>> residents = new LinkedHashMap<Long, List<Long>>();
    private final Set<String> billedPeriods = new LinkedHashSet<String>();
    private final Map<Long, List<BigDecimal>> allocations = new LinkedHashMap<Long, List<BigDecimal>>();
    private final List<ResidentAbsenceSnapshot> residentSnapshots =
            new ArrayList<ResidentAbsenceSnapshot>();
    private final Set<String> approvedLeaves = new LinkedHashSet<String>();
    private final Map<Long, AbsenceWarningDto> warnings = new LinkedHashMap<Long, AbsenceWarningDto>();
    private WarningConfigDto warningConfig = new WarningConfigDto(3, 7, true, LocalDateTime.now());
    private long nextReadingId = 1L;
    private long nextBillId = 1000L;
    private final Map<Long, VisitorRegistrationDto> visitors =
            new LinkedHashMap<Long, VisitorRegistrationDto>();
    private final Map<Long, Long> activeRooms = new LinkedHashMap<Long, Long>();
    private final Map<Long, NoticeExtraDto> notices = new LinkedHashMap<Long, NoticeExtraDto>();
    private final Map<Long, String> buildings = new LinkedHashMap<Long, String>();
    private final Map<Long, Long> roomBuildings = new LinkedHashMap<Long, Long>();
    private final Map<Long, String> phones = new LinkedHashMap<Long, String>();
    private int expirableAnnouncements;
    private long nextWarningId = 1L;
    private long nextVisitorId = 1L;
    private final Map<Long, HygieneDetailDto> inspections = new LinkedHashMap<Long, HygieneDetailDto>();
    private final Map<Long, List<HygieneItemScoreDto>> itemScores =
            new LinkedHashMap<Long, List<HygieneItemScoreDto>>();
    private final Map<Long, HygieneTaskDto> tasks = new LinkedHashMap<Long, HygieneTaskDto>();
    private long nextInspectionId = 1L;
    private long nextTaskId = 1L;
    private final List<AccessRecordExtDto> accessRecords = new ArrayList<AccessRecordExtDto>();
    private final Map<Long, RepairEntryPermitDto> repairOrders =
            new LinkedHashMap<Long, RepairEntryPermitDto>();
    private final Map<Long, Long> repairReporters = new LinkedHashMap<Long, Long>();
    private AccessPolicyDto accessPolicy =
            new AccessPolicyDto(LocalTime.of(23, 0), LocalTime.of(5, 0), LocalDateTime.now());

    /** 注册一个可用房间，供录入校验使用。 */
    public synchronized void addRoom(long roomId, String buildingCode, String roomNo) {
        rooms.put(Long.valueOf(roomId), buildingCode + "/" + roomNo);
    }

    /** 设置房间当前在住学生，用于分摊。 */
    public synchronized void setResidents(long roomId, Long... studentUserIds) {
        List<Long> value = new ArrayList<Long>();
        Collections.addAll(value, studentUserIds);
        Collections.sort(value);
        residents.put(Long.valueOf(roomId), value);
    }

    /** 把某条读数标记为已生成账单，用于验证锁定分支。 */
    public synchronized void attachBill(long readingId, long billId) {
        MeterReadingDto old = readings.get(Long.valueOf(readingId));
        if (old == null) return;
        readings.put(Long.valueOf(readingId), copyWithBill(old, Long.valueOf(billId)));
    }

    /** 已写入的某张账单的全部分摊金额，供测试断言。 */
    public synchronized List<BigDecimal> allocationsOf(long billId) {
        List<BigDecimal> value = allocations.get(Long.valueOf(billId));
        return value == null ? Collections.<BigDecimal>emptyList()
                : Collections.unmodifiableList(value);
    }

    /** 已生成的账单张数，供测试断言。 */
    public synchronized int billCount() { return allocations.size(); }

    @Override
    public synchronized DormPage<MeterReadingDto> listMeterReadings(Connection c, DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<MeterReadingDto> rows = new ArrayList<MeterReadingDto>();
        for (MeterReadingDto item : readings.values()) {
            boolean roomMatches = q.getRoomId() == null || q.getRoomId().longValue() == item.getRoomId();
            if (roomMatches && InMemoryDormSupport.matches(q.getKeyword(),
                    item.getRoomNo(), item.getBuildingCode())) {
                rows.add(item);
            }
        }
        return InMemoryDormSupport.page(rows, q);
    }

    @Override
    public synchronized MeterReadingDto saveMeterReading(Connection c, MeterReadingRequest request,
                                                         long actorUserId) {
        String location = rooms.get(Long.valueOf(request.getRoomId()));
        if (location == null) {
            throw new DormRepositoryException(DormExtCommands.ROOM_NOT_FOUND, "房间不存在");
        }
        MeterReadingDto existing = find(request.getRoomId(), request.getPeriodStart(),
                request.getPeriodEnd());
        if (existing != null && existing.isLocked()) {
            throw new DormRepositoryException(DormExtCommands.METER_LOCKED,
                    "该账期已生成账单，读数不可修改");
        }
        long id = existing == null ? nextReadingId++ : existing.getId();
        String[] parts = location.split("/", 2);
        MeterReadingDto value = new MeterReadingDto(id, request.getRoomId(), parts[0], parts[1],
                request.getPeriodStart(), request.getPeriodEnd(),
                request.getElectricityUnits(), request.getWaterUnits(),
                request.getElectricityPrice(), request.getWaterPrice(),
                actorUserId, LocalDateTime.now(), null);
        readings.put(Long.valueOf(id), value);
        return value;
    }

    @Override
    public synchronized List<MeterReadingDto> pendingReadings(Connection c, Long roomId,
                                                              LocalDate periodStart,
                                                              LocalDate periodEnd) {
        List<MeterReadingDto> rows = new ArrayList<MeterReadingDto>();
        for (MeterReadingDto item : readings.values()) {
            boolean roomMatches = roomId == null || roomId.longValue() == item.getRoomId();
            if (roomMatches && !item.isLocked()
                    && item.getPeriodStart().equals(periodStart)
                    && item.getPeriodEnd().equals(periodEnd)) {
                rows.add(item);
            }
        }
        return rows;
    }

    @Override
    public synchronized List<Long> activeResidents(Connection c, long roomId) {
        List<Long> value = residents.get(Long.valueOf(roomId));
        return value == null ? Collections.<Long>emptyList() : new ArrayList<Long>(value);
    }

    @Override
    public synchronized boolean billExists(Connection c, long roomId, LocalDate periodStart,
                                           LocalDate periodEnd) {
        return billedPeriods.contains(key(roomId, periodStart, periodEnd));
    }

    @Override
    public synchronized long createBill(Connection c, MeterReadingDto reading,
                                        BigDecimal totalAmount, LocalDateTime dueAt,
                                        long actorUserId) {
        long billId = nextBillId++;
        billedPeriods.add(key(reading.getRoomId(), reading.getPeriodStart(), reading.getPeriodEnd()));
        allocations.put(Long.valueOf(billId), new ArrayList<BigDecimal>());
        return billId;
    }

    @Override
    public synchronized void createAllocation(Connection c, long billId, long studentUserId,
                                              BigDecimal amount) {
        List<BigDecimal> target = allocations.get(Long.valueOf(billId));
        if (target == null) {
            throw new DormRepositoryException(DormExtCommands.INTERNAL_ERROR, "账单不存在");
        }
        target.add(amount);
    }

    @Override
    public synchronized void linkReadingToBill(Connection c, long readingId, long billId) {
        attachBill(readingId, billId);
    }

    // ================= 未归预警 =================

    /** 登记一名在住学生及其最近进出时间，作为扫描输入。 */
    public synchronized void addResident(long studentUserId, long roomId,
                                         LocalDateTime lastExitAt, LocalDateTime lastEntryAt) {
        residentSnapshots.add(new ResidentAbsenceSnapshot(studentUserId, roomId,
                lastExitAt, lastEntryAt));
    }

    /** 登记一条已批准的请假，用于验证豁免分支。 */
    public synchronized void addApprovedLeave(long studentUserId, LocalDate date) {
        approvedLeaves.add(studentUserId + "|" + date);
    }

    @Override
    public synchronized List<ResidentAbsenceSnapshot> residentsForScan(Connection c) {
        return new ArrayList<ResidentAbsenceSnapshot>(residentSnapshots);
    }

    @Override
    public synchronized boolean hasApprovedLeave(Connection c, long studentUserId, LocalDate date) {
        return approvedLeaves.contains(studentUserId + "|" + date);
    }

    @Override
    public synchronized AbsenceWarningDto saveWarning(Connection c, long studentUserId, long roomId,
                                                      LocalDate scanDate, LocalDateTime lastLeaveAt,
                                                      int absenceDays, String warningLevel) {
        AbsenceWarningDto existing = findWarningByStudent(studentUserId, scanDate);
        long id = existing == null ? nextWarningId++ : existing.getId();
        String[] parts = location(roomId);
        AbsenceWarningDto value = new AbsenceWarningDto(id, studentUserId, roomId, parts[0],
                parts[1], scanDate, lastLeaveAt, absenceDays, warningLevel,
                existing == null ? AbsenceWarningDto.STATUS_PENDING : existing.getHandleStatus(),
                existing == null ? null : existing.getNotifiedTeacherId(),
                existing == null ? null : existing.getNotifiedAt(),
                existing == null ? null : existing.getNote());
        warnings.put(Long.valueOf(id), value);
        return value;
    }

    @Override
    public synchronized DormPage<AbsenceWarningDto> listWarnings(Connection c, DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<AbsenceWarningDto> rows = new ArrayList<AbsenceWarningDto>();
        for (AbsenceWarningDto item : warnings.values()) {
            boolean roomMatches = q.getRoomId() == null || q.getRoomId().longValue() == item.getRoomId();
            if (roomMatches && InMemoryDormSupport.status(q.getStatus(), item.getHandleStatus())
                    && InMemoryDormSupport.matches(q.getKeyword(),
                            item.getRoomNo(), item.getBuildingCode(), item.getWarningLevel())) {
                rows.add(item);
            }
        }
        return InMemoryDormSupport.page(rows, q);
    }

    @Override
    public synchronized AbsenceWarningDto findWarning(Connection c, long warningId) {
        return warnings.get(Long.valueOf(warningId));
    }

    @Override
    public synchronized AbsenceWarningDto updateWarningStatus(Connection c, long warningId,
                                                              String status, Long teacherUserId,
                                                              LocalDateTime notifiedAt, String note) {
        AbsenceWarningDto old = warnings.get(Long.valueOf(warningId));
        if (old == null) {
            throw new DormRepositoryException(DormExtCommands.WARNING_NOT_FOUND, "预警不存在");
        }
        AbsenceWarningDto value = new AbsenceWarningDto(old.getId(), old.getStudentUserId(),
                old.getRoomId(), old.getBuildingCode(), old.getRoomNo(), old.getScanDate(),
                old.getLastLeaveAt(), old.getAbsenceDays(), old.getWarningLevel(), status,
                teacherUserId, notifiedAt, note);
        warnings.put(Long.valueOf(warningId), value);
        return value;
    }

    @Override
    public synchronized WarningConfigDto loadWarningConfig(Connection c) { return warningConfig; }

    @Override
    public synchronized WarningConfigDto saveWarningConfig(Connection c, WarningConfigRequest request,
                                                           long actorUserId) {
        warningConfig = new WarningConfigDto(request.getWarnDays(), request.getNotifyDays(),
                request.isExemptOnLeave(), LocalDateTime.now());
        return warningConfig;
    }

    // ================= 在宿状态、门禁、入内许可、房间删除 =================

    /** 登记一条进出流水，供晚归判定测试使用。 */
    public synchronized void addAccessRecord(long id, long studentUserId, String type,
                                             LocalDateTime occurredAt, String doorName) {
        accessRecords.add(new AccessRecordExtDto(id, studentUserId, type, occurredAt,
                doorName, false));
    }

    /** 登记一张报修单，供入内许可测试使用。 */
    public synchronized void addRepairOrder(long orderId, long reporterId, long roomId,
                                            String category, String status) {
        String[] parts = location(roomId);
        repairReporters.put(Long.valueOf(orderId), Long.valueOf(reporterId));
        repairOrders.put(Long.valueOf(orderId), new RepairEntryPermitDto(orderId, roomId,
                parts[0], parts[1], category, status, LocalDateTime.now(), false, null,
                phones.get(Long.valueOf(reporterId))));
    }

    @Override
    public synchronized List<StayStatusDto> stayStatusRows(Connection c, Long studentUserId) {
        List<StayStatusDto> rows = new ArrayList<StayStatusDto>();
        for (ResidentAbsenceSnapshot item : residentSnapshots) {
            if (studentUserId != null && studentUserId.longValue() != item.getStudentUserId()) {
                continue;
            }
            String[] parts = location(item.getRoomId());
            rows.add(new StayStatusDto(item.getStudentUserId(), item.getRoomId(), parts[0],
                    parts[1], null, item.getLastExitAt(), item.getLastEntryAt()));
        }
        return rows;
    }

    @Override
    public synchronized AccessPolicyDto loadAccessPolicy(Connection c) { return accessPolicy; }

    @Override
    public synchronized AccessPolicyDto saveAccessPolicy(Connection c, AccessPolicyRequest request,
                                                         long actorUserId) {
        accessPolicy = new AccessPolicyDto(request.getCurfewTime(), request.getDawnTime(),
                LocalDateTime.now());
        return accessPolicy;
    }

    @Override
    public synchronized DormPage<AccessRecordExtDto> listAccessRecords(Connection c,
                                                                       long studentUserId,
                                                                       DormPageQuery query,
                                                                       AccessPolicyDto policy) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<AccessRecordExtDto> rows = new ArrayList<AccessRecordExtDto>();
        for (AccessRecordExtDto item : accessRecords) {
            if (item.getStudentUserId() != studentUserId) continue;
            if (!InMemoryDormSupport.matches(q.getKeyword(), item.getDoorName(),
                    item.getRecordType())) {
                continue;
            }
            if (q.getStatus() != null && !q.getStatus().trim().isEmpty()
                    && !q.getStatus().trim().equals(item.getRecordType())) {
                continue;
            }
            rows.add(new AccessRecordExtDto(item.getId(), item.getStudentUserId(),
                    item.getRecordType(), item.getOccurredAt(), item.getDoorName(),
                    DormStayRules.isLateReturn(item.getRecordType(), item.getOccurredAt(),
                            policy.getCurfewTime(), policy.getDawnTime())));
        }
        return InMemoryDormSupport.page(rows, q);
    }

    /** 登记学生账号里的手机号；不设置即表示该生没留电话。 */
    public synchronized void setPhone(long userId, String phone) {
        phones.put(Long.valueOf(userId), phone);
    }

    @Override
    public synchronized String reporterPhoneOf(Connection c, long repairOrderId) {
        Long reporter = repairReporters.get(Long.valueOf(repairOrderId));
        return reporter == null ? null : phones.get(reporter);
    }

    @Override
    public synchronized Long repairReporterOf(Connection c, long repairOrderId) {
        return repairReporters.get(Long.valueOf(repairOrderId));
    }

    @Override
    public synchronized RepairEntryPermitDto saveRepairPermit(Connection c,
                                                              RepairEntryPermitRequest request,
                                                              long actorUserId) {
        RepairEntryPermitDto old = repairOrders.get(Long.valueOf(request.getRepairOrderId()));
        if (old == null) {
            throw new DormRepositoryException(DormExtCommands.REPAIR_NOT_FOUND, "报修单不存在");
        }
        Long reporter = repairReporters.get(Long.valueOf(request.getRepairOrderId()));
        RepairEntryPermitDto value = new RepairEntryPermitDto(old.getRepairOrderId(),
                old.getRoomId(), old.getBuildingCode(), old.getRoomNo(), old.getCategory(),
                old.getOrderStatus(), old.getSubmittedAt(), request.isAllowEnter(),
                request.getNote(), reporter == null ? null : phones.get(reporter));
        repairOrders.put(Long.valueOf(request.getRepairOrderId()), value);
        return value;
    }

    @Override
    public synchronized DormPage<RepairEntryPermitDto> listRepairPermits(Connection c,
                                                                         long studentUserId,
                                                                         DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<RepairEntryPermitDto> rows = new ArrayList<RepairEntryPermitDto>();
        for (Map.Entry<Long, RepairEntryPermitDto> entry : repairOrders.entrySet()) {
            Long reporter = repairReporters.get(entry.getKey());
            if (reporter != null && reporter.longValue() == studentUserId
                    && InMemoryDormSupport.status(q.getStatus(), entry.getValue().getOrderStatus())) {
                rows.add(entry.getValue());
            }
        }
        return InMemoryDormSupport.page(rows, q);
    }

    @Override
    public synchronized int activeResidentCount(Connection c, long roomId) {
        int count = 0;
        for (ResidentAbsenceSnapshot item : residentSnapshots) {
            if (item.getRoomId() == roomId) count++;
        }
        return count;
    }

    @Override
    public synchronized int roomReferenceCount(Connection c, long roomId) {
        int count = activeResidentCount(c, roomId);
        for (MeterReadingDto item : readings.values()) {
            if (item.getRoomId() == roomId) count++;
        }
        for (HygieneTaskDto item : tasks.values()) {
            if (item.getRoomId() == roomId) count++;
        }
        for (AbsenceWarningDto item : warnings.values()) {
            if (item.getRoomId() == roomId) count++;
        }
        for (VisitorRegistrationDto item : visitors.values()) {
            if (item.getRoomId() == roomId) count++;
        }
        for (RepairEntryPermitDto item : repairOrders.values()) {
            if (item.getRoomId() == roomId) count++;
        }
        return count;
    }

    @Override
    public synchronized void deleteRoom(Connection c, long roomId) {
        if (rooms.remove(Long.valueOf(roomId)) == null) {
            throw new DormRepositoryException(DormExtCommands.ROOM_NOT_FOUND, "房间不存在");
        }
    }

    // ================= 卫生分项与检查任务 =================

    @Override
    public synchronized long createInspection(Connection c, long roomId, long inspectorId,
                                              LocalDateTime inspectedAt, BigDecimal totalScore,
                                              String result, String status,
                                              String issueDescription) {
        long id = nextInspectionId++;
        String[] parts = location(roomId);
        boolean rectify = DormHygieneRules.needRectify(totalScore);
        inspections.put(Long.valueOf(id), new HygieneDetailDto(id, roomId, parts[0], parts[1],
                inspectorId, inspectedAt, totalScore, DormHygieneRules.level(totalScore), rectify,
                rectify ? DormHygieneRules.recheckDate(inspectedAt.toLocalDate()) : null,
                issueDescription, null));
        return id;
    }

    @Override
    public synchronized void saveItemScores(Connection c, long inspectionId,
                                            List<HygieneItemScoreDto> items) {
        itemScores.put(Long.valueOf(inspectionId), new ArrayList<HygieneItemScoreDto>(items));
    }

    @Override
    public synchronized HygieneDetailDto findInspectionDetail(Connection c, long inspectionId) {
        HygieneDetailDto base = inspections.get(Long.valueOf(inspectionId));
        if (base == null) return null;
        return new HygieneDetailDto(base.getInspectionId(), base.getRoomId(),
                base.getBuildingCode(), base.getRoomNo(), base.getInspectorId(),
                base.getInspectedAt(), base.getTotalScore(), base.getScoreLevel(),
                base.isNeedRectify(), base.getRecheckDate(), base.getIssueDescription(),
                itemScores.get(Long.valueOf(inspectionId)));
    }

    @Override
    public synchronized List<Long> roomsForWeeklyTask(Connection c, Long buildingId) {
        return new ArrayList<Long>(rooms.keySet());
    }

    /** 直接塞一条指定编号、类型、日期和状态的检查任务，用于验证列表的待办顺序。 */
    public synchronized void addTask(long id, long roomId, String taskType, LocalDate planDate,
                                     String status) {
        String[] parts = location(roomId);
        tasks.put(Long.valueOf(id), new HygieneTaskDto(id, roomId, parts[0], parts[1], taskType,
                planDate, status, null, null));
        if (id >= nextTaskId) nextTaskId = id + 1;
    }

    @Override
    public synchronized boolean createTaskIfAbsent(Connection c, long roomId, String taskType,
                                                   LocalDate planDate, Long sourceInspectionId) {
        for (HygieneTaskDto item : tasks.values()) {
            if (item.getRoomId() == roomId && item.getTaskType().equals(taskType)
                    && item.getPlanDate().equals(planDate)) {
                return false;
            }
        }
        long id = nextTaskId++;
        String[] parts = location(roomId);
        tasks.put(Long.valueOf(id), new HygieneTaskDto(id, roomId, parts[0], parts[1], taskType,
                planDate, HygieneTaskDto.STATUS_PENDING, null, sourceInspectionId));
        return true;
    }

    @Override
    public synchronized int markTasksDone(Connection c, long roomId, LocalDate onOrBefore,
                                          long inspectionId) {
        int done = 0;
        for (Map.Entry<Long, HygieneTaskDto> entry : tasks.entrySet()) {
            HygieneTaskDto item = entry.getValue();
            if (item.getRoomId() == roomId
                    && HygieneTaskDto.STATUS_PENDING.equals(item.getStatus())
                    && !item.getPlanDate().isAfter(onOrBefore)) {
                entry.setValue(new HygieneTaskDto(item.getId(), item.getRoomId(),
                        item.getBuildingCode(), item.getRoomNo(), item.getTaskType(),
                        item.getPlanDate(), HygieneTaskDto.STATUS_DONE,
                        Long.valueOf(inspectionId), item.getSourceInspectionId()));
                done++;
            }
        }
        return done;
    }

    @Override
    public synchronized DormPage<HygieneTaskDto> listTasks(Connection c, DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<HygieneTaskDto> rows = new ArrayList<HygieneTaskDto>();
        for (HygieneTaskDto item : tasks.values()) {
            boolean roomMatches = q.getRoomId() == null || q.getRoomId().longValue() == item.getRoomId();
            if (roomMatches && InMemoryDormSupport.status(q.getStatus(), item.getStatus())
                    && InMemoryDormSupport.matches(q.getKeyword(),
                            item.getRoomNo(), item.getBuildingCode(), item.getTaskType())) {
                rows.add(item);
            }
        }
        Collections.sort(rows, TASK_ORDER);
        return InMemoryDormSupport.page(rows, q);
    }

    /** 与 MySQL 实现同一套待办顺序：待检查优先、复查优先、计划日期从早到晚。 */
    private static final Comparator<HygieneTaskDto> TASK_ORDER = new Comparator<HygieneTaskDto>() {
        @Override
        public int compare(HygieneTaskDto left, HygieneTaskDto right) {
            int byStatus = rank(left.getStatus()) - rank(right.getStatus());
            if (byStatus != 0) return byStatus;
            int byType = (left.isRecheck() ? 0 : 1) - (right.isRecheck() ? 0 : 1);
            if (byType != 0) return byType;
            int byDate = left.getPlanDate().compareTo(right.getPlanDate());
            return byDate != 0 ? byDate
                    : Long.valueOf(left.getId()).compareTo(Long.valueOf(right.getId()));
        }

        private int rank(String status) {
            if (HygieneTaskDto.STATUS_PENDING.equals(status)) return 0;
            if (HygieneTaskDto.STATUS_SKIPPED.equals(status)) return 1;
            return 2;
        }
    };

    // ================= 外来人员登记 =================

    /** 登记一名学生当前的在住房间，供来访登记解析。 */
    public synchronized void addAccommodation(long studentUserId, long roomId) {
        activeRooms.put(Long.valueOf(studentUserId), Long.valueOf(roomId));
    }

    @Override
    public synchronized Long activeRoomOf(Connection c, long studentUserId) {
        return activeRooms.get(Long.valueOf(studentUserId));
    }

    @Override
    public synchronized VisitorRegistrationDto createVisitor(Connection c, long studentUserId,
                                                             long roomId,
                                                             VisitorRegistrationRequest request) {
        long id = nextVisitorId++;
        String[] parts = location(roomId);
        VisitorRegistrationDto value = new VisitorRegistrationDto(id, studentUserId, roomId,
                parts[0], parts[1], request.getVisitorName().trim(),
                VisitorRegistrationDto.mask(request.getVisitorIdCard()),
                request.getVisitorPhone(), request.getVisitReason().trim(),
                request.getStartAt(), request.getEndAt(), LocalDateTime.now(),
                VisitorRegistrationDto.STATUS_PENDING, null, null, null);
        visitors.put(Long.valueOf(id), value);
        return value;
    }

    @Override
    public synchronized DormPage<VisitorRegistrationDto> listVisitors(Connection c,
                                                                      DormPageQuery query,
                                                                      Long studentUserId) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<VisitorRegistrationDto> rows = new ArrayList<VisitorRegistrationDto>();
        for (VisitorRegistrationDto item : visitors.values()) {
            boolean mine = studentUserId == null
                    || studentUserId.longValue() == item.getStudentUserId();
            if (mine && InMemoryDormSupport.status(q.getStatus(), item.getAuditStatus())
                    && InMemoryDormSupport.matches(q.getKeyword(),
                            item.getVisitorName(), item.getVisitReason(), item.getRoomNo())) {
                rows.add(item);
            }
        }
        return InMemoryDormSupport.page(rows, q);
    }

    @Override
    public synchronized VisitorRegistrationDto findVisitor(Connection c, long registrationId) {
        return visitors.get(Long.valueOf(registrationId));
    }

    @Override
    public synchronized VisitorRegistrationDto updateVisitorStatus(Connection c,
                                                                   long registrationId,
                                                                   String status, Long auditorId,
                                                                   LocalDateTime auditedAt,
                                                                   String remark) {
        VisitorRegistrationDto old = visitors.get(Long.valueOf(registrationId));
        if (old == null) {
            throw new DormRepositoryException(DormExtCommands.VISITOR_NOT_FOUND, "来访登记不存在");
        }
        VisitorRegistrationDto value = new VisitorRegistrationDto(old.getId(),
                old.getStudentUserId(), old.getRoomId(), old.getBuildingCode(), old.getRoomNo(),
                old.getVisitorName(), old.getVisitorIdCardMasked(), old.getVisitorPhone(),
                old.getVisitReason(), old.getStartAt(), old.getEndAt(), old.getSubmittedAt(),
                status, auditorId, auditedAt, remark);
        visitors.put(Long.valueOf(registrationId), value);
        return value;
    }

    private AbsenceWarningDto findWarningByStudent(long studentUserId, LocalDate scanDate) {
        for (AbsenceWarningDto item : warnings.values()) {
            if (item.getStudentUserId() == studentUserId && item.getScanDate().equals(scanDate)) {
                return item;
            }
        }
        return null;
    }

    private String[] location(long roomId) {
        String value = rooms.get(Long.valueOf(roomId));
        return value == null ? new String[] { "?", "?" } : value.split("/", 2);
    }

    private MeterReadingDto find(long roomId, LocalDate start, LocalDate end) {
        for (MeterReadingDto item : readings.values()) {
            if (item.getRoomId() == roomId && item.getPeriodStart().equals(start)
                    && item.getPeriodEnd().equals(end)) {
                return item;
            }
        }
        return null;
    }

    private static String key(long roomId, LocalDate start, LocalDate end) {
        return roomId + "|" + start + "|" + end;
    }

    private static MeterReadingDto copyWithBill(MeterReadingDto source, Long billId) {
        return new MeterReadingDto(source.getId(), source.getRoomId(), source.getBuildingCode(),
                source.getRoomNo(), source.getPeriodStart(), source.getPeriodEnd(),
                source.getElectricityUnits(), source.getWaterUnits(),
                source.getElectricityPrice(), source.getWaterPrice(),
                source.getRecordedBy(), source.getRecordedAt(), billId);
    }

    // ---- 公告类型、范围与置顶 ----

    /** 登记一栋楼，供公告范围解析使用。 */
    public synchronized void addBuilding(long buildingId, String buildingCode) {
        buildings.put(Long.valueOf(buildingId), buildingCode);
    }

    /** 把房间挂到楼栋下；公告按楼栋投放时要靠它反查。 */
    public synchronized void setRoomBuilding(long roomId, long buildingId) {
        roomBuildings.put(Long.valueOf(roomId), Long.valueOf(buildingId));
    }

    /** 造一条宿舍公告；扩展属性用默认值，模拟 main 已发布、尚未设置过的公告。 */
    public synchronized void addNotice(long announcementId, String title, String status) {
        notices.put(Long.valueOf(announcementId), new NoticeExtraDto(announcementId, title,
                title + " 正文", status, LocalDateTime.now().minusDays(1), null, 90L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ALL, null, null, null, null,
                false, null));
    }

    @Override
    public synchronized DormPage<NoticeExtraDto> listNotices(Connection c, DormPageQuery query,
                                                             boolean manageView,
                                                             Long viewerRoomId) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<NoticeExtraDto> rows = new ArrayList<NoticeExtraDto>();
        for (NoticeExtraDto item : notices.values()) {
            if (!manageView) {
                if (!"PUBLISHED".equals(item.getStatus())) continue;
                if (!visible(item, viewerRoomId)) continue;
            }
            if (!InMemoryDormSupport.matches(q.getKeyword(), item.getTitle(), item.getContent())) {
                continue;
            }
            String filter = q.getStatus() == null ? null : q.getStatus().trim();
            if (filter != null && !filter.isEmpty()) {
                String actual = manageView ? item.getStatus() : item.getNoticeType();
                if (!filter.equals(actual)) continue;
            }
            rows.add(item);
        }
        Collections.sort(rows, new Comparator<NoticeExtraDto>() {
            @Override
            public int compare(NoticeExtraDto left, NoticeExtraDto right) {
                if (left.isPinned() != right.isPinned()) return left.isPinned() ? -1 : 1;
                return Long.valueOf(right.getAnnouncementId())
                        .compareTo(Long.valueOf(left.getAnnouncementId()));
            }
        });
        return InMemoryDormSupport.page(rows, q);
    }

    @Override
    public synchronized NoticeExtraDto findNotice(Connection c, long announcementId) {
        return notices.get(Long.valueOf(announcementId));
    }

    @Override
    public synchronized NoticeExtraDto saveNoticeExtra(Connection c, NoticeExtraRequest request,
                                                       long actorUserId) {
        NoticeExtraDto old = notices.get(Long.valueOf(request.getAnnouncementId()));
        if (old == null) {
            throw new DormRepositoryException(DormExtCommands.NOTICE_NOT_FOUND, "宿舍公告不存在");
        }
        Long buildingId = request.getScopeBuildingId();
        Long roomId = request.getScopeRoomId();
        if (roomId != null && buildingId == null) {
            buildingId = roomBuildings.get(roomId);
        }
        String buildingCode = buildingId == null ? null : buildings.get(buildingId);
        String roomNo = roomId == null ? null : location(roomId.longValue())[1];
        // 重复保存同一条已置顶公告不刷新置顶时刻，取消置顶则清空。
        LocalDateTime pinnedAt = !request.isPinned() ? null
                : (old.isPinned() && old.getPinnedAt() != null ? old.getPinnedAt()
                        : LocalDateTime.now());
        NoticeExtraDto value = new NoticeExtraDto(old.getAnnouncementId(), old.getTitle(),
                old.getContent(), old.getStatus(), old.getPublishAt(), old.getExpireAt(),
                old.getPublisherId(), request.getNoticeType(), request.getScopeType(),
                buildingId, roomId, buildingCode, roomNo, request.isPinned(), pinnedAt);
        notices.put(Long.valueOf(old.getAnnouncementId()), value);
        return value;
    }

    private boolean visible(NoticeExtraDto item, Long viewerRoomId) {
        if (NoticeExtraDto.SCOPE_ALL.equals(item.getScopeType())) return true;
        if (viewerRoomId == null) return false;
        if (NoticeExtraDto.SCOPE_ROOM.equals(item.getScopeType())) {
            return viewerRoomId.equals(item.getScopeRoomId());
        }
        Long viewerBuilding = roomBuildings.get(viewerRoomId);
        return viewerBuilding != null && viewerBuilding.equals(item.getScopeBuildingId());
    }

    // ---- 定时任务专用 ----

    /** 内存版没有公告表；用一个计数器模拟「本次下架了几条」，供服务层单测走通链路。 */
    public synchronized void setExpirableAnnouncements(int count) {
        this.expirableAnnouncements = Math.max(0, count);
    }

    @Override
    public synchronized int expireDormAnnouncements(Connection c, LocalDateTime now) {
        int affected = expirableAnnouncements;
        expirableAnnouncements = 0;
        return affected;
    }

    @Override
    public synchronized List<AbsenceWarningDto> pendingSevereWarnings(Connection c,
                                                                      LocalDate onOrBefore) {
        LocalDate limit = onOrBefore == null ? LocalDate.now() : onOrBefore;
        List<AbsenceWarningDto> rows = new ArrayList<AbsenceWarningDto>();
        for (AbsenceWarningDto item : warnings.values()) {
            if (!AbsenceWarningDto.LEVEL_SEVERE.equals(item.getWarningLevel())) continue;
            if (!AbsenceWarningDto.STATUS_PENDING.equals(item.getHandleStatus())) continue;
            if (item.getScanDate() != null && item.getScanDate().isAfter(limit)) continue;
            rows.add(item);
        }
        return rows;
    }
}
