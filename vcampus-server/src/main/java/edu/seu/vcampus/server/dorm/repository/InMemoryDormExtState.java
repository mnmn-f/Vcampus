package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.ext.*;
import java.math.BigDecimal;
import java.util.*;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/** Shared state for the small, synchronized in-memory extension repositories. */
final class InMemoryDormExtState {
    final Map<Long, MeterReadingDto> readings = new LinkedHashMap<Long, MeterReadingDto>();
    final Map<Long, String> rooms = new LinkedHashMap<Long, String>();
    final Map<Long, List<Long>> residents = new LinkedHashMap<Long, List<Long>>();
    final Set<String> billedPeriods = new LinkedHashSet<String>();
    final Map<Long, List<BigDecimal>> allocations = new LinkedHashMap<Long, List<BigDecimal>>();
    final List<ResidentAbsenceSnapshot> residentSnapshots = new ArrayList<ResidentAbsenceSnapshot>();
    final Set<String> approvedLeaves = new LinkedHashSet<String>();
    final Map<Long, AbsenceWarningDto> warnings = new LinkedHashMap<Long, AbsenceWarningDto>();
    WarningConfigDto warningConfig = new WarningConfigDto(3, 7, true, LocalDateTime.now());
    long nextReadingId = 1L;
    long nextBillId = 1000L;
    final Map<Long, VisitorRegistrationDto> visitors = new LinkedHashMap<Long, VisitorRegistrationDto>();
    final Map<Long, Long> activeRooms = new LinkedHashMap<Long, Long>();
    final Map<Long, NoticeExtraDto> notices = new LinkedHashMap<Long, NoticeExtraDto>();
    final Map<Long, String> buildings = new LinkedHashMap<Long, String>();
    final Map<Long, Long> roomBuildings = new LinkedHashMap<Long, Long>();
    final Map<Long, String> phones = new LinkedHashMap<Long, String>();
    int expirableAnnouncements;
    long nextWarningId = 1L;
    long nextVisitorId = 1L;
    final Map<Long, HygieneDetailDto> inspections = new LinkedHashMap<Long, HygieneDetailDto>();
    final Map<Long, List<HygieneItemScoreDto>> itemScores = new LinkedHashMap<Long, List<HygieneItemScoreDto>>();
    final Map<Long, HygieneTaskDto> tasks = new LinkedHashMap<Long, HygieneTaskDto>();
    long nextInspectionId = 1L;
    long nextTaskId = 1L;
    final List<AccessRecordExtDto> accessRecords = new ArrayList<AccessRecordExtDto>();
    final Map<Long, RepairEntryPermitDto> repairOrders = new LinkedHashMap<Long, RepairEntryPermitDto>();
    final Map<Long, Long> repairReporters = new LinkedHashMap<Long, Long>();
    final Map<Long, Long> repairHandlers = new LinkedHashMap<Long, Long>();
    final Map<Long, String> repairPriorities = new LinkedHashMap<Long, String>();
    final Map<Long, String> repairWorkers = new LinkedHashMap<Long, String>();
    AccessPolicyDto accessPolicy = new AccessPolicyDto(LocalTime.of(23, 0), LocalTime.of(5, 0), LocalDateTime.now());
}
