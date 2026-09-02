package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningConfigRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningHandleRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanRequest;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanResultDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormAbsenceRules;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 连续未归预警：天数判定、阈值分级、请假豁免、通知与核实状态机。 */
public final class DormAbsenceWarningTest {
    private static final LocalDateTime LEFT_AT = LocalDateTime.of(2026, 9, 1, 20, 0);

    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;

    @Before
    public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addRoom(10L, "D1", "101");
        repository.addResident(11L, 10L, LEFT_AT, null);
        service = new DormExtService(repository);
        manager = session(90L, Role.DORM_MANAGER);
        student = session(11L, Role.STUDENT);
    }

    // ---------- 判定规则（纯函数） ----------

    @Test
    public void absenceCountsFromLastExit() {
        assertEquals(4, DormAbsenceRules.absenceDays(LEFT_AT, null, LocalDate.of(2026, 9, 5)));
    }

    @Test
    public void returningHomeResetsAbsence() {
        LocalDateTime back = LocalDateTime.of(2026, 9, 2, 8, 0);
        assertEquals(0, DormAbsenceRules.absenceDays(LEFT_AT, back, LocalDate.of(2026, 9, 5)));
    }

    @Test
    public void sameDayExitIsNotAbsence() {
        assertEquals(0, DormAbsenceRules.absenceDays(LEFT_AT, null, LocalDate.of(2026, 9, 1)));
    }

    @Test
    public void noAccessHistoryIsNotAbsence() {
        assertEquals(0, DormAbsenceRules.absenceDays(null, null, LocalDate.of(2026, 9, 5)));
    }

    @Test
    public void approvedLeaveOutranksDayCount() {
        assertEquals(AbsenceWarningDto.LEVEL_EXEMPT, DormAbsenceRules.level(30, 7, true));
        assertEquals(AbsenceWarningDto.LEVEL_SEVERE, DormAbsenceRules.level(30, 7, false));
        assertEquals(AbsenceWarningDto.LEVEL_NORMAL, DormAbsenceRules.level(4, 7, false));
    }

    // ---------- 扫描 ----------

    @Test
    public void scanCreatesNormalWarningAboveWarnThreshold() {
        WarningScanResultDto result = service.scanAbsences(manager, scan(2026, 9, 5));
        assertEquals(1, result.getResidentsScanned());
        assertEquals(1, result.getNormalCount());
        assertEquals(0, result.getSevereCount());
        assertEquals(1, result.getWarningCount());
    }

    @Test
    public void scanSkipsResidentsBelowWarnThreshold() {
        WarningScanResultDto result = service.scanAbsences(manager, scan(2026, 9, 2));
        assertEquals(1, result.getResidentsScanned());
        assertEquals("未归 1 天未达默认阈值 3 天", 0, result.getWarningCount());
    }

    @Test
    public void scanEscalatesToSevereAtNotifyThreshold() {
        WarningScanResultDto result = service.scanAbsences(manager, scan(2026, 9, 10));
        assertEquals(1, result.getSevereCount());
        assertEquals(0, result.getNormalCount());
    }

    @Test
    public void approvedLeaveMakesWarningExempt() {
        repository.addApprovedLeave(11L, LocalDate.of(2026, 9, 10));
        WarningScanResultDto result = service.scanAbsences(manager, scan(2026, 9, 10));
        assertEquals(1, result.getExemptCount());
        assertEquals(0, result.getSevereCount());
    }

    @Test
    public void rescanUpdatesInsteadOfDuplicating() {
        service.scanAbsences(manager, scan(2026, 9, 5));
        service.scanAbsences(manager, scan(2026, 9, 5));
        assertEquals("同一学生同一扫描日只留一条", 1L,
                service.warnings(manager, DormPageQuery.all()).getTotalElements());
    }

    @Test
    public void rescanKeepsHandledStatus() {
        service.scanAbsences(manager, scan(2026, 9, 5));
        long id = firstWarning().getId();
        service.notifyWarning(manager, new WarningHandleRequest(id, Long.valueOf(70L), "已电话通知"));
        service.scanAbsences(manager, scan(2026, 9, 5));
        assertEquals("重复扫描不应把已处理的预警打回待处理",
                AbsenceWarningDto.STATUS_NOTIFIED, firstWarning().getHandleStatus());
    }

    // ---------- 处理状态机 ----------

    @Test
    public void notifyMarksWarningAsNotified() {
        service.scanAbsences(manager, scan(2026, 9, 10));
        long id = firstWarning().getId();
        AbsenceWarningDto notified = service.notifyWarning(manager,
                new WarningHandleRequest(id, Long.valueOf(70L), "已联系辅导员"));
        assertEquals(AbsenceWarningDto.STATUS_NOTIFIED, notified.getHandleStatus());
        assertEquals(Long.valueOf(70L), notified.getNotifiedTeacherId());
        assertNotNull(notified.getNotifiedAt());
    }

    @Test
    public void notifyRequiresAnExplicitTeacher() {
        service.scanAbsences(manager, scan(2026, 9, 10));
        long id = firstWarning().getId();
        try {
            service.notifyWarning(manager, new WarningHandleRequest(id, null, null));
            fail("系统里没有学生到辅导员的映射，接收人必须显式给出");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.INVALID_INPUT, ex.getResultCode());
        }
    }

    @Test
    public void verifiedWarningRejectsFurtherNotification() {
        service.scanAbsences(manager, scan(2026, 9, 10));
        long id = firstWarning().getId();
        service.verifyWarning(manager, new WarningHandleRequest(id, null, "本人已返校"));
        try {
            service.notifyWarning(manager, new WarningHandleRequest(id, Long.valueOf(70L), null));
            fail("已核实的预警不应再通知");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.WARNING_INVALID_STATE, ex.getResultCode());
        }
    }

    @Test
    public void notifyingUnknownWarningIsRejected() {
        try {
            service.notifyWarning(manager, new WarningHandleRequest(999L, Long.valueOf(70L), null));
            fail("不存在的预警应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.WARNING_NOT_FOUND, ex.getResultCode());
        }
    }

    // ---------- 阈值配置 ----------

    @Test
    public void configChangeTakesEffectOnNextScan() {
        service.saveWarningConfig(manager, new WarningConfigRequest(1, 2, true));
        WarningScanResultDto result = service.scanAbsences(manager, scan(2026, 9, 2));
        assertEquals("阈值降到 1 天后，未归 1 天也要预警", 1, result.getWarningCount());
        assertEquals("未归 1 天未达通知阈值 2 天，仍是一般", 1, result.getNormalCount());
        assertEquals(0, result.getSevereCount());
    }

    @Test
    public void configRejectsNotifyDaysBelowWarnDays() {
        try {
            service.saveWarningConfig(manager, new WarningConfigRequest(7, 3, true));
            fail("通知天数不能小于预警天数");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.CONFIG_INVALID, ex.getResultCode());
        }
    }

    @Test
    public void exemptionCanBeSwitchedOff() {
        service.saveWarningConfig(manager, new WarningConfigRequest(3, 7, false));
        repository.addApprovedLeave(11L, LocalDate.of(2026, 9, 10));
        WarningScanResultDto result = service.scanAbsences(manager, scan(2026, 9, 10));
        assertEquals("关闭豁免后请假也照常升严重", 1, result.getSevereCount());
        assertEquals(0, result.getExemptCount());
    }

    @Test
    public void defaultConfigIsReadable() {
        WarningConfigDto config = service.warningConfig(manager);
        assertEquals(3, config.getWarnDays());
        assertEquals(7, config.getNotifyDays());
        assertTrue(config.isExemptOnLeave());
    }

    // ---------- 权限 ----------

    @Test
    public void studentCannotScanOrRead() {
        try {
            service.scanAbsences(student, scan(2026, 9, 5));
            fail("学生没有宿舍治理权限");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
        try {
            service.warnings(student, DormPageQuery.all());
            fail("学生不能查看全量预警");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    @Test
    public void warningCarriesRoomLocation() {
        service.scanAbsences(manager, scan(2026, 9, 5));
        AbsenceWarningDto warning = firstWarning();
        assertEquals("D1", warning.getBuildingCode());
        assertEquals("101", warning.getRoomNo());
        assertEquals(LEFT_AT, warning.getLastLeaveAt());
        assertNull(warning.getNotifiedTeacherId());
    }

    private AbsenceWarningDto firstWarning() {
        return service.warnings(manager, DormPageQuery.all()).getItems().get(0);
    }

    private static WarningScanRequest scan(int year, int month, int day) {
        return new WarningScanRequest(LocalDate.of(year, month, day));
    }

    private static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }
}
