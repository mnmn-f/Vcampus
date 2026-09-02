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
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 连续未归预警：扫描、处理状态机、阈值配置与权限。 */
public final class DormAbsenceWarningTest {
    private static final LocalDateTime LEFT_AT = LocalDateTime.of(2026, 9, 1, 20, 0);
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addRoom(10L, "D1", "101");
        repository.addResident(11L, 10L, LEFT_AT, null);
        service = new DormExtService(repository);
        manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
        student = DormExtTestSupport.session(11L, Role.STUDENT);
    }

    @Test public void scanCreatesNormalWarningAboveWarnThreshold() {
        WarningScanResultDto r = service.scanAbsences(manager, scan(2026, 9, 5));
        assertEquals(1, r.getResidentsScanned()); assertEquals(1, r.getNormalCount());
        assertEquals(0, r.getSevereCount()); assertEquals(1, r.getWarningCount());
    }
    @Test public void scanSkipsResidentsBelowWarnThreshold() {
        assertEquals(0, service.scanAbsences(manager, scan(2026, 9, 2)).getWarningCount());
    }
    @Test public void scanEscalatesToSevereAtNotifyThreshold() {
        WarningScanResultDto r = service.scanAbsences(manager, scan(2026, 9, 10));
        assertEquals(1, r.getSevereCount()); assertEquals(0, r.getNormalCount());
    }
    @Test public void approvedLeaveMakesWarningExempt() {
        repository.addApprovedLeave(11L, LocalDate.of(2026, 9, 10));
        WarningScanResultDto r = service.scanAbsences(manager, scan(2026, 9, 10));
        assertEquals(1, r.getExemptCount()); assertEquals(0, r.getSevereCount());
    }
    @Test public void rescanUpdatesInsteadOfDuplicating() {
        service.scanAbsences(manager, scan(2026, 9, 5));
        service.scanAbsences(manager, scan(2026, 9, 5));
        assertEquals(1L, service.warnings(manager, DormPageQuery.all()).getTotalElements());
    }
    @Test public void rescanKeepsHandledStatus() {
        service.scanAbsences(manager, scan(2026, 9, 5));
        service.notifyWarning(manager, new WarningHandleRequest(firstWarning().getId(), 70L, "已电话通知"));
        service.scanAbsences(manager, scan(2026, 9, 5));
        assertEquals(AbsenceWarningDto.STATUS_NOTIFIED, firstWarning().getHandleStatus());
    }
    @Test public void notifyMarksWarningAsNotified() {
        service.scanAbsences(manager, scan(2026, 9, 10));
        AbsenceWarningDto r = service.notifyWarning(manager,
                new WarningHandleRequest(firstWarning().getId(), 70L, "已联系辅导员"));
        assertEquals(AbsenceWarningDto.STATUS_NOTIFIED, r.getHandleStatus());
        assertEquals(Long.valueOf(70L), r.getNotifiedTeacherId()); assertNotNull(r.getNotifiedAt());
    }
    @Test public void notifyRequiresAnExplicitTeacher() {
        service.scanAbsences(manager, scan(2026, 9, 10));
        DormExtTestSupport.assertCode(DormExtCommands.INVALID_INPUT, new DormExtTestSupport.Action() {
            public void run() { service.notifyWarning(manager,
                    new WarningHandleRequest(firstWarning().getId(), null, null)); }
        });
    }
    @Test public void verifiedWarningRejectsFurtherNotification() {
        service.scanAbsences(manager, scan(2026, 9, 10));
        final long id = firstWarning().getId();
        service.verifyWarning(manager, new WarningHandleRequest(id, null, "本人已返校"));
        DormExtTestSupport.assertCode(DormExtCommands.WARNING_INVALID_STATE, new DormExtTestSupport.Action() {
            public void run() { service.notifyWarning(manager, new WarningHandleRequest(id, 70L, null)); }
        });
    }
    @Test public void notifyingUnknownWarningIsRejected() {
        DormExtTestSupport.assertCode(DormExtCommands.WARNING_NOT_FOUND, new DormExtTestSupport.Action() {
            public void run() { service.notifyWarning(manager, new WarningHandleRequest(999L, 70L, null)); }
        });
    }
    @Test public void configChangeTakesEffectOnNextScan() {
        service.saveWarningConfig(manager, new WarningConfigRequest(1, 2, true));
        WarningScanResultDto r = service.scanAbsences(manager, scan(2026, 9, 2));
        assertEquals(1, r.getWarningCount()); assertEquals(1, r.getNormalCount());
        assertEquals(0, r.getSevereCount());
    }
    @Test public void configRejectsNotifyDaysBelowWarnDays() {
        DormExtTestSupport.assertCode(DormExtCommands.CONFIG_INVALID, new DormExtTestSupport.Action() {
            public void run() { service.saveWarningConfig(manager, new WarningConfigRequest(7, 3, true)); }
        });
    }
    @Test public void exemptionCanBeSwitchedOff() {
        service.saveWarningConfig(manager, new WarningConfigRequest(3, 7, false));
        repository.addApprovedLeave(11L, LocalDate.of(2026, 9, 10));
        WarningScanResultDto r = service.scanAbsences(manager, scan(2026, 9, 10));
        assertEquals(1, r.getSevereCount()); assertEquals(0, r.getExemptCount());
    }
    @Test public void defaultConfigIsReadable() {
        WarningConfigDto c = service.warningConfig(manager);
        assertEquals(3, c.getWarnDays()); assertEquals(7, c.getNotifyDays()); assertTrue(c.isExemptOnLeave());
    }
    @Test public void studentCannotScanOrRead() {
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.scanAbsences(student, scan(2026, 9, 5)); }
        });
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.warnings(student, DormPageQuery.all()); }
        });
    }
    @Test public void warningCarriesRoomLocation() {
        service.scanAbsences(manager, scan(2026, 9, 5));
        AbsenceWarningDto w = firstWarning();
        assertEquals("D1", w.getBuildingCode()); assertEquals("101", w.getRoomNo());
        assertEquals(LEFT_AT, w.getLastLeaveAt()); assertNull(w.getNotifiedTeacherId());
    }

    private AbsenceWarningDto firstWarning() {
        return service.warnings(manager, DormPageQuery.all()).getItems().get(0);
    }
    private static WarningScanRequest scan(int y, int m, int d) {
        return new WarningScanRequest(LocalDate.of(y, m, d));
    }
}
