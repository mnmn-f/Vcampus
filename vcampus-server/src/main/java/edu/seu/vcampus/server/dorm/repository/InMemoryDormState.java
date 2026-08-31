package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 内存仓储共享状态；由按职责拆分的仓储实现共同使用。 */
final class InMemoryDormState {
    final Map<Long, DormBuildingDto> buildings = new LinkedHashMap<Long, DormBuildingDto>();
    final Map<Long, DormRoomDto> rooms = new LinkedHashMap<Long, DormRoomDto>();
    final Map<Long, DormBedDto> beds = new LinkedHashMap<Long, DormBedDto>();
    final Map<Long, AccommodationDto> accommodations =
            new LinkedHashMap<Long, AccommodationDto>();
    final Map<Long, AccommodationRequestDto> requests =
            new LinkedHashMap<Long, AccommodationRequestDto>();
    final Map<Long, AccessRecordDto> access = new LinkedHashMap<Long, AccessRecordDto>();
    final Map<Long, LateReturnAlertDto> alerts = new LinkedHashMap<Long, LateReturnAlertDto>();
    final Map<Long, HygieneInspectionDto> hygiene =
            new LinkedHashMap<Long, HygieneInspectionDto>();
    final Map<Long, RepairOrderDto> repairs = new LinkedHashMap<Long, RepairOrderDto>();
    final Map<Long, LeaveRequestDto> leaves = new LinkedHashMap<Long, LeaveRequestDto>();
    final Map<Long, UtilityBillDto> bills = new LinkedHashMap<Long, UtilityBillDto>();
    final Map<Long, Long> billOwners = new LinkedHashMap<Long, Long>();
    final Map<Long, DormAnnouncementDto> announcements =
            new LinkedHashMap<Long, DormAnnouncementDto>();
    final Set<Long> activeStudents = new HashSet<Long>();
    long nextAccommodation = 1L;
    long nextBuilding = 1L;
    long nextRoom = 1L;
    long nextBed = 1L;
    long nextLeave = 1L;
    long nextRequest = 1L;
    long nextAccess = 1L;
    long nextAlert = 1L;
    long nextHygiene = 1L;
    long nextRepair = 1L;
    long nextAnnouncement = 1L;
    long nextPayment = 1L;
}
