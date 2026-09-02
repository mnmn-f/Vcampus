package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.ext.schedule.DormScheduler;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.dorm.service.DormSchedulerStatus;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 五项宿舍后台任务、按需触发与启停状态。 */
public final class DormSchedulerExecutionTest {
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository(); repository.addRoom(10L, "D1", "101");
        repository.addResident(11L, 10L, LocalDateTime.now().minusDays(10), null);
        service = new DormExtService(repository); manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
    }
    @Test public void schedulerRegistersFiveTasksAndSchedules() {
        DormScheduler s = new DormScheduler(service, null); assertEquals(5, s.taskCount()); String all = joined(s.describe());
        assertTrue(all.contains(DormSchedulerStatus.TASK_ABSENCE_SCAN)); assertTrue(all.contains(DormSchedulerStatus.TASK_WARNING_NOTIFY));
        assertTrue(all.contains(DormSchedulerStatus.TASK_HYGIENE_TASK)); assertTrue(all.contains(DormSchedulerStatus.TASK_NOTICE_EXPIRE));
        assertTrue(all.contains(DormSchedulerStatus.TASK_BILL_GENERATE)); assertTrue(all.contains("每日 08:00"));
        assertTrue(all.contains("每日 08:10")); assertTrue(all.contains("每周一 09:00"));
        assertTrue(all.contains("每日 00:00")); assertTrue(all.contains("每月 1 日 09:00"));
    }
    @Test public void statusReflectsSchedulerLifecycle() {
        assertFalse(service.status(manager).isSchedulerRunning()); DormScheduler s = new DormScheduler(service, null); s.start();
        try { DormExtStatusDto status = service.status(manager); assertTrue(status.isSchedulerRunning()); assertEquals(5, status.getScheduledTasks().size()); }
        finally { s.stop(); }
        assertFalse(service.status(manager).isSchedulerRunning());
    }
    @Test public void runAllExecutesTasksWithoutFailures() {
        repository.setExpirableAnnouncements(2); new DormScheduler(service, null).runAllNow(); DormSchedulerStatus status = service.schedulerStatus();
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_ABSENCE_SCAN)); assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_WARNING_NOTIFY));
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_HYGIENE_TASK)); assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_NOTICE_EXPIRE));
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_BILL_GENERATE));
        for (String line : status.describe()) assertFalse(line, line.contains("尚未执行"));
    }
    @Test public void taskResultsContainDomainSummaries() {
        repository.setExpirableAnnouncements(3); new DormScheduler(service, null).runAllNow();
        assertTrue(service.schedulerStatus().lastResult(DormSchedulerStatus.TASK_ABSENCE_SCAN).contains("扫描 1 人"));
        List<AbsenceWarningDto> severe = service.pendingSevereWarnings(LocalDate.now()); assertEquals(1, severe.size());
        assertEquals(11L, severe.get(0).getStudentUserId());
        assertTrue(service.schedulerStatus().lastResult(DormSchedulerStatus.TASK_WARNING_NOTIFY).contains("待通知严重未归 1 条"));
        assertTrue(service.schedulerStatus().lastResult(DormSchedulerStatus.TASK_WARNING_NOTIFY).contains("D1101"));
        assertEquals("下架过期公告 3 条", service.schedulerStatus().lastResult(DormSchedulerStatus.TASK_NOTICE_EXPIRE));
    }
    @Test public void hygieneGenerationIsIdempotentAndBillingCanSkip() {
        new DormScheduler(service, null).runAllNow();
        assertTrue(service.schedulerStatus().lastResult(DormSchedulerStatus.TASK_HYGIENE_TASK).contains("新建 1 条"));
        new DormScheduler(service, null).runAllNow();
        assertTrue(service.schedulerStatus().lastResult(DormSchedulerStatus.TASK_HYGIENE_TASK).contains("已存在 1 条"));
        String skipped = service.schedulerStatus().lastResult(DormSchedulerStatus.TASK_BILL_GENERATE);
        assertTrue(skipped.startsWith("跳过：")); assertTrue(skipped.contains(DormScheduler.KEY_OPERATOR));
        new DormScheduler(service, 90L).runAllNow();
        assertTrue(service.schedulerStatus().lastResult(DormSchedulerStatus.TASK_BILL_GENERATE).startsWith("跳过："));
    }
    @Test public void manualRunSupportsOneOrAllTasks() {
        DormScheduler s = new DormScheduler(service, null); service.attachTaskRunner(s);
        service.runScheduledTask(manager, DormSchedulerStatus.TASK_NOTICE_EXPIRE);
        assertEquals(1, service.schedulerStatus().runCount(DormSchedulerStatus.TASK_NOTICE_EXPIRE));
        assertEquals(0, service.schedulerStatus().runCount(DormSchedulerStatus.TASK_ABSENCE_SCAN));
        DormExtStatusDto status = service.runScheduledTask(manager, DormSchedulerStatus.TASK_ABSENCE_SCAN);
        assertEquals(5, status.getScheduledTasks().size()); assertTrue(joined(status.getScheduledTasks()).contains("扫描 1 人"));
        service.runScheduledTask(manager, null);
        for (String line : service.schedulerStatus().describe()) assertFalse(line.contains("尚未执行"));
    }
    @Test public void manualRunRejectsUnknownOrUnavailableAndStudents() {
        DormScheduler s = new DormScheduler(service, null); service.attachTaskRunner(s);
        DormExtTestSupport.assertCode(DormExtCommands.INVALID_INPUT, new DormExtTestSupport.Action() {
            public void run() { service.runScheduledTask(manager, "nope"); }
        });
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.runScheduledTask(DormExtTestSupport.session(11L, Role.STUDENT), null); }
        });
        DormExtService noScheduler = new DormExtService(new InMemoryDormExtRepository());
        DormExtTestSupport.assertCode(DormExtCommands.SCHEDULER_UNAVAILABLE, new DormExtTestSupport.Action() {
            public void run() { noScheduler.runScheduledTask(manager, null); }
        });
    }
    @Test public void schedulerEnabledPropertyIsRestored() {
        String old = System.getProperty(DormScheduler.KEY_ENABLED);
        try { System.setProperty(DormScheduler.KEY_ENABLED, "false"); assertFalse(DormScheduler.enabled());
            System.setProperty(DormScheduler.KEY_ENABLED, "true"); assertTrue(DormScheduler.enabled()); }
        finally { if (old == null) System.clearProperty(DormScheduler.KEY_ENABLED); else System.setProperty(DormScheduler.KEY_ENABLED, old); }
    }
    private static String joined(List<String> lines) { StringBuilder b = new StringBuilder(); for (String line : lines) b.append(line).append('\n'); return b.toString(); }
}
