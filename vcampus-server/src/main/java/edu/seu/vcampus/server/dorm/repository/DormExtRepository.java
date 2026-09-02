package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** Persistence boundary for the dormitory extension. */
public interface DormExtRepository {
    DormPage<MeterReadingDto> listMeterReadings(Connection c, DormPageQuery q) throws SQLException;
    MeterReadingDto saveMeterReading(Connection c, MeterReadingRequest r, long actor) throws SQLException;
    List<MeterReadingDto> pendingReadings(Connection c, Long roomId, LocalDate start, LocalDate end) throws SQLException;
    List<Long> activeResidents(Connection c, long roomId) throws SQLException;
    boolean billExists(Connection c, long roomId, LocalDate start, LocalDate end) throws SQLException;
    long createBill(Connection c, MeterReadingDto reading, BigDecimal amount, LocalDateTime dueAt, long actor) throws SQLException;
    void createAllocation(Connection c, long billId, long studentId, BigDecimal amount) throws SQLException;
    void linkReadingToBill(Connection c, long readingId, long billId) throws SQLException;

    List<ResidentAbsenceSnapshot> residentsForScan(Connection c) throws SQLException;
    boolean hasApprovedLeave(Connection c, long studentId, LocalDate date) throws SQLException;
    AbsenceWarningDto saveWarning(Connection c, long studentId, long roomId, LocalDate date,
                                  LocalDateTime lastLeaveAt, int days, String level) throws SQLException;
    DormPage<AbsenceWarningDto> listWarnings(Connection c, DormPageQuery q) throws SQLException;
    AbsenceWarningDto findWarning(Connection c, long warningId) throws SQLException;
    AbsenceWarningDto updateWarningStatus(Connection c, long warningId, String status,
                                          Long teacherId, LocalDateTime notifiedAt, String note) throws SQLException;
    WarningConfigDto loadWarningConfig(Connection c) throws SQLException;
    WarningConfigDto saveWarningConfig(Connection c, WarningConfigRequest r, long actor) throws SQLException;

    Long activeRoomOf(Connection c, long studentId) throws SQLException;
    VisitorRegistrationDto createVisitor(Connection c, long studentId, long roomId,
                                         VisitorRegistrationRequest r) throws SQLException;
    DormPage<VisitorRegistrationDto> listVisitors(Connection c, DormPageQuery q, Long studentId) throws SQLException;
    VisitorRegistrationDto findVisitor(Connection c, long registrationId) throws SQLException;
    VisitorRegistrationDto updateVisitorStatus(Connection c, long registrationId, String status,
                                               Long auditorId, LocalDateTime auditedAt, String remark) throws SQLException;

    long createInspection(Connection c, long roomId, long inspectorId, LocalDateTime inspectedAt,
                          BigDecimal total, String result, String status, String issue) throws SQLException;
    void saveItemScores(Connection c, long inspectionId, List<HygieneItemScoreDto> items) throws SQLException;
    HygieneDetailDto findInspectionDetail(Connection c, long inspectionId) throws SQLException;
    List<Long> roomsForWeeklyTask(Connection c, Long buildingId) throws SQLException;
    boolean createTaskIfAbsent(Connection c, long roomId, String taskType, LocalDate planDate,
                               Long sourceInspectionId) throws SQLException;
    int markTasksDone(Connection c, long roomId, LocalDate onOrBefore, long inspectionId) throws SQLException;
    DormPage<HygieneTaskDto> listTasks(Connection c, DormPageQuery q) throws SQLException;

    List<StayStatusDto> stayStatusRows(Connection c, Long studentId) throws SQLException;
    AccessPolicyDto loadAccessPolicy(Connection c) throws SQLException;
    AccessPolicyDto saveAccessPolicy(Connection c, AccessPolicyRequest r, long actor) throws SQLException;
    DormPage<AccessRecordExtDto> listAccessRecords(Connection c, long studentId, DormPageQuery q,
                                                   AccessPolicyDto policy) throws SQLException;
    Long repairReporterOf(Connection c, long repairOrderId) throws SQLException;
    String reporterPhoneOf(Connection c, long repairOrderId) throws SQLException;
    RepairEntryPermitDto saveRepairPermit(Connection c, RepairEntryPermitRequest r, long actor) throws SQLException;
    DormPage<RepairEntryPermitDto> listRepairPermits(Connection c, long studentId, DormPageQuery q) throws SQLException;
    int activeResidentCount(Connection c, long roomId) throws SQLException;
    int roomReferenceCount(Connection c, long roomId) throws SQLException;
    void deleteRoom(Connection c, long roomId) throws SQLException;

    DormPage<NoticeExtraDto> listNotices(Connection c, DormPageQuery q, boolean manageView, Long viewerRoomId) throws SQLException;
    NoticeExtraDto findNotice(Connection c, long announcementId) throws SQLException;
    NoticeExtraDto saveNoticeExtra(Connection c, NoticeExtraRequest r, long actor) throws SQLException;
    int expireDormAnnouncements(Connection c, LocalDateTime now) throws SQLException;
    List<AbsenceWarningDto> pendingSevereWarnings(Connection c, LocalDate onOrBefore) throws SQLException;
}
