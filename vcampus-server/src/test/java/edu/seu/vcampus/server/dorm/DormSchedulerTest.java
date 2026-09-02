package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.ext.schedule.DormScheduleTimes;
import edu.seu.vcampus.server.dorm.ext.schedule.DormScheduler;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.dorm.service.DormSchedulerStatus;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 定时任务：触发时刻计算、运行台账、五项任务的端到端链路。 */
public final class DormSchedulerTest {
    private static final LocalTime AT_0800 = LocalTime.of(8, 0);

    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;

    @Before
    public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addRoom(10L, "D1", "101");
        repository.addResident(11L, 10L, LocalDateTime.now().minusDays(10), null);
        service = new DormExtService(repository);
        manager = session(90L);
    }

    // ---------- 下一次触发时刻（纯函数） ----------

    @Test
    public void dailyStaysTodayWhenTimeIsStillAhead() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 1, 7, 30);
        assertEquals(LocalDateTime.of(2026, 9, 1, 8, 0),
                DormScheduleTimes.nextDaily(now, AT_0800));
    }

    @Test
    public void dailyRollsToTomorrowWhenTimeHasPassed() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 1, 8, 30);
        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 0),
                DormScheduleTimes.nextDaily(now, AT_0800));
    }

    @Test
    public void dailyOnTheDotGoesToTomorrow() {
        // 任务跑完立刻重排时，当前时刻正好等于计划时刻；必须顺延，否则同一秒内连跑两次。
        LocalDateTime now = LocalDateTime.of(2026, 9, 1, 8, 0);
        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 0),
                DormScheduleTimes.nextDaily(now, AT_0800));
    }

    @Test
    public void dailyCrossesMonthBoundary() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 9, 0);
        assertEquals(LocalDateTime.of(2026, 10, 1, 8, 0),
                DormScheduleTimes.nextDaily(now, AT_0800));
    }

    @Test
    public void weeklyPicksTheComingMonday() {
        // 2026-09-01 是周二
        LocalDateTime now = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime next = DormScheduleTimes.nextWeekly(now, DayOfWeek.MONDAY, LocalTime.of(9, 0));
        assertEquals(LocalDateTime.of(2026, 9, 7, 9, 0), next);
        assertEquals(DayOfWeek.MONDAY, next.getDayOfWeek());
    }

    @Test
    public void weeklyOnMondayBeforeTimeStaysToday() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 7, 8, 0);
        assertEquals(LocalDateTime.of(2026, 9, 7, 9, 0),
                DormScheduleTimes.nextWeekly(now, DayOfWeek.MONDAY, LocalTime.of(9, 0)));
    }

    @Test
    public void weeklyOnMondayAfterTimeGoesNextWeek() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 7, 9, 30);
        assertEquals(LocalDateTime.of(2026, 9, 14, 9, 0),
                DormScheduleTimes.nextWeekly(now, DayOfWeek.MONDAY, LocalTime.of(9, 0)));
    }

    @Test
    public void monthlyPicksFirstDayOfNextMonth() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 15, 10, 0);
        assertEquals(LocalDateTime.of(2026, 10, 1, 9, 0),
                DormScheduleTimes.nextMonthly(now, 1, LocalTime.of(9, 0)));
    }

    @Test
    public void monthlyStaysTodayWhenFirstDayHasNotPassed() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 1, 8, 0);
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0),
                DormScheduleTimes.nextMonthly(now, 1, LocalTime.of(9, 0)));
    }

    @Test
    public void monthlyCrossesYearBoundary() {
        LocalDateTime now = LocalDateTime.of(2026, 12, 20, 10, 0);
        assertEquals(LocalDateTime.of(2027, 1, 1, 9, 0),
                DormScheduleTimes.nextMonthly(now, 1, LocalTime.of(9, 0)));
    }

    @Test
    public void monthlyClampsToTheLastDayOfAShortMonth() {
        // 31 号的月度任务在 2 月落到 28 号，而不是整月跳过。
        LocalDateTime now = LocalDateTime.of(2027, 2, 5, 10, 0);
        assertEquals(LocalDateTime.of(2027, 2, 28, 9, 0),
                DormScheduleTimes.nextMonthly(now, 31, LocalTime.of(9, 0)));
    }

    @Test
    public void millisBetweenCountsForward() {
        assertEquals(90000L, DormScheduleTimes.millisBetween(
                LocalDateTime.of(2026, 9, 1, 8, 0, 0), LocalDateTime.of(2026, 9, 1, 8, 1, 30)));
    }

    @Test
    public void millisBetweenNeverGoesNegative() {
        assertEquals(0L, DormScheduleTimes.millisBetween(
                LocalDateTime.of(2026, 9, 1, 8, 0), LocalDateTime.of(2026, 9, 1, 7, 0)));
    }

    @Test
    public void invalidDayOfMonthIsRejected() {
        try {
            DormScheduleTimes.nextMonthly(LocalDateTime.now(), 0, AT_0800);
            fail("应当拒绝非法的日期");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    // ---------- 运行台账 ----------

    @Test
    public void newlyRegisteredTaskReportsNotRunYet() {
        DormSchedulerStatus status = new DormSchedulerStatus();
        status.register("demo", "每日 08:00");
        assertEquals(1, status.taskCount());
        assertTrue(status.describe().get(0).contains("尚未执行"));
        assertFalse(status.isRunning());
    }

    @Test
    public void registeringTwiceDoesNotDuplicateTheEntry() {
        DormSchedulerStatus status = new DormSchedulerStatus();
        status.register("demo", "每日 08:00");
        status.register("demo", "每日 09:00");
        assertEquals(1, status.taskCount());
        assertTrue(status.describe().get(0).contains("每日 09:00"));
    }

    @Test
    public void successAndFailureAreCountedSeparately() {
        DormSchedulerStatus status = new DormSchedulerStatus();
        status.register("demo", "每日 08:00");
        status.recordSuccess("demo", "扫描 3 人");
        status.recordFailure("demo", "数据库不可用");
        assertEquals(2, status.runCount("demo"));
        assertEquals(1, status.failureCount("demo"));
        assertEquals("数据库不可用", status.lastResult("demo"));
        assertTrue(status.describe().get(0).contains("失败"));
    }

    @Test
    public void skippingIsNotCountedAsFailure() {
        DormSchedulerStatus status = new DormSchedulerStatus();
        status.register("demo", "每月 1 日 09:00");
        status.recordSkipped("demo", "未配置记账人");
        assertEquals(0, status.runCount("demo"));
        assertEquals(0, status.failureCount("demo"));
        assertTrue(status.lastResult("demo").startsWith("跳过："));
    }

    @Test
    public void unknownTaskIsRejected() {
        DormSchedulerStatus status = new DormSchedulerStatus();
        try {
            status.recordSuccess("nope", "x");
            fail("应当拒绝未注册的任务名");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("nope"));
        }
    }

    // ---------- 五项任务的接线与链路 ----------

    @Test
    public void schedulerRegistersTheFiveDocumentedTasks() {
        DormScheduler scheduler = new DormScheduler(service, null);
        assertEquals(5, scheduler.taskCount());
        String all = joined(scheduler.describe());
        assertTrue(all.contains(DormSchedulerStatus.TASK_ABSENCE_SCAN));
        assertTrue(all.contains(DormSchedulerStatus.TASK_WARNING_NOTIFY));
        assertTrue(all.contains(DormSchedulerStatus.TASK_HYGIENE_TASK));
        assertTrue(all.contains(DormSchedulerStatus.TASK_NOTICE_EXPIRE));
        assertTrue(all.contains(DormSchedulerStatus.TASK_BILL_GENERATE));
    }

    @Test
    public void everyTaskDeclaresItsSchedule() {
        DormScheduler scheduler = new DormScheduler(service, null);
        String all = joined(scheduler.describe());
        assertTrue(all.contains("每日 08:00"));
        assertTrue(all.contains("每日 08:10"));
        assertTrue(all.contains("每周一 09:00"));
        assertTrue(all.contains("每日 00:00"));
        assertTrue(all.contains("每月 1 日 09:00"));
    }

    @Test
    public void statusReportsSchedulerStoppedBeforeStart() {
        assertFalse(service.status(manager).isSchedulerRunning());
    }

    @Test
    public void statusReportsSchedulerRunningAfterStart() {
        DormScheduler scheduler = new DormScheduler(service, null);
        scheduler.start();
        try {
            DormExtStatusDto status = service.status(manager);
            assertTrue(status.isSchedulerRunning());
            assertEquals(5, status.getScheduledTasks().size());
        } finally {
            scheduler.stop();
        }
        assertFalse(service.status(manager).isSchedulerRunning());
    }

    @Test
    public void runningEveryTaskOnceLeavesNoFailure() {
        repository.setExpirableAnnouncements(2);
        DormScheduler scheduler = new DormScheduler(service, Long.valueOf(90L));
        scheduler.runAllNow();
        DormSchedulerStatus status = service.schedulerStatus();
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_ABSENCE_SCAN));
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_WARNING_NOTIFY));
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_HYGIENE_TASK));
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_NOTICE_EXPIRE));
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_BILL_GENERATE));
        for (String line : status.describe()) {
            assertFalse(line, line.contains("尚未执行"));
        }
    }

    @Test
    public void absenceScanTaskWritesWarnings() {
        new DormScheduler(service, null).runAllNow();
        assertTrue(service.schedulerStatus()
                .lastResult(DormSchedulerStatus.TASK_ABSENCE_SCAN).contains("扫描 1 人"));
        List<AbsenceWarningDto> severe = service.pendingSevereWarnings(LocalDate.now());
        assertEquals(1, severe.size());
        assertEquals(11L, severe.get(0).getStudentUserId());
    }

    @Test
    public void notifyTaskSummarisesPendingSevereWarnings() {
        new DormScheduler(service, null).runAllNow();
        String summary = service.schedulerStatus()
                .lastResult(DormSchedulerStatus.TASK_WARNING_NOTIFY);
        assertTrue(summary, summary.contains("待通知严重未归 1 条"));
        assertTrue(summary, summary.contains("D1101"));
    }

    @Test
    public void noticeExpireTaskReportsAffectedRows() {
        repository.setExpirableAnnouncements(3);
        new DormScheduler(service, null).runAllNow();
        assertEquals("下架过期公告 3 条", service.schedulerStatus()
                .lastResult(DormSchedulerStatus.TASK_NOTICE_EXPIRE));
    }

    @Test
    public void hygieneTaskCoversEveryRoomOnce() {
        new DormScheduler(service, null).runAllNow();
        assertTrue(service.schedulerStatus()
                .lastResult(DormSchedulerStatus.TASK_HYGIENE_TASK).contains("新建 1 条"));
        // 再跑一次不应重复建任务
        new DormScheduler(service, null).runAllNow();
        assertTrue(service.schedulerStatus()
                .lastResult(DormSchedulerStatus.TASK_HYGIENE_TASK).contains("已存在 1 条"));
    }

    @Test
    public void billTaskIsSkippedWhenNoOperatorConfigured() {
        new DormScheduler(service, null).runAllNow();
        DormSchedulerStatus status = service.schedulerStatus();
        String last = status.lastResult(DormSchedulerStatus.TASK_BILL_GENERATE);
        assertTrue(last, last.startsWith("跳过："));
        assertTrue(last, last.contains(DormScheduler.KEY_OPERATOR));
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_BILL_GENERATE));
    }

    @Test
    public void billTaskIsSkippedWhenNothingToBill() {
        new DormScheduler(service, Long.valueOf(90L)).runAllNow();
        DormSchedulerStatus status = service.schedulerStatus();
        // 当月没有抄表是常态，不能记成故障。
        assertTrue(status.lastResult(DormSchedulerStatus.TASK_BILL_GENERATE).startsWith("跳过："));
        assertEquals(0, status.failureCount(DormSchedulerStatus.TASK_BILL_GENERATE));
    }

    // ---------- 按需触发 ----------

    @Test
    public void runningOneTaskLeavesTheOthersUntouched() {
        DormScheduler scheduler = new DormScheduler(service, null);
        service.attachTaskRunner(scheduler);
        service.runScheduledTask(manager, DormSchedulerStatus.TASK_NOTICE_EXPIRE);
        DormSchedulerStatus status = service.schedulerStatus();
        assertEquals(1, status.runCount(DormSchedulerStatus.TASK_NOTICE_EXPIRE));
        assertEquals("没点到的任务不应被顺带执行", 0,
                status.runCount(DormSchedulerStatus.TASK_ABSENCE_SCAN));
    }

    @Test
    public void runningWithoutATaskNameRunsThemAll() {
        DormScheduler scheduler = new DormScheduler(service, null);
        service.attachTaskRunner(scheduler);
        service.runScheduledTask(manager, null);
        for (String line : service.schedulerStatus().describe()) {
            assertFalse(line, line.contains("尚未执行"));
        }
    }

    @Test
    public void manualRunReturnsTheRefreshedStatus() {
        DormScheduler scheduler = new DormScheduler(service, null);
        service.attachTaskRunner(scheduler);
        DormExtStatusDto status =
                service.runScheduledTask(manager, DormSchedulerStatus.TASK_ABSENCE_SCAN);
        assertEquals(5, status.getScheduledTasks().size());
        assertTrue("触发后要能立刻在返回的状态里看到结果",
                joined(status.getScheduledTasks()).contains("扫描 1 人"));
    }

    @Test
    public void unknownTaskNameIsRejected() {
        DormScheduler scheduler = new DormScheduler(service, null);
        service.attachTaskRunner(scheduler);
        try {
            service.runScheduledTask(manager, "nope");
            fail("未知任务名应当被拒绝");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.INVALID_INPUT, ex.getResultCode());
        }
    }

    @Test
    public void manualRunNeedsARunningScheduler() {
        // 调度器被 -Dvcampus.dorm.scheduler.enabled=false 关掉时不能假装成功。
        try {
            service.runScheduledTask(manager, null);
            fail("没有调度器时应当明确报错");
        } catch (DormException ex) {
            assertEquals(DormExtCommands.SCHEDULER_UNAVAILABLE, ex.getResultCode());
        }
    }

    @Test
    public void studentsCannotTriggerTasks() {
        service.attachTaskRunner(new DormScheduler(service, null));
        try {
            service.runScheduledTask(session(11L, Role.STUDENT), null);
            fail("学生不应能触发定时任务");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    @Test
    public void schedulerCanBeDisabledByProperty() {
        String old = System.getProperty(DormScheduler.KEY_ENABLED);
        try {
            System.setProperty(DormScheduler.KEY_ENABLED, "false");
            assertFalse(DormScheduler.enabled());
            System.setProperty(DormScheduler.KEY_ENABLED, "true");
            assertTrue(DormScheduler.enabled());
        } finally {
            if (old == null) {
                System.clearProperty(DormScheduler.KEY_ENABLED);
            } else {
                System.setProperty(DormScheduler.KEY_ENABLED, old);
            }
        }
    }

    private static String joined(List<String> lines) {
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            text.append(line).append('\n');
        }
        return text.toString();
    }

    private static SessionContext session(long userId) {
        return session(userId, Role.DORM_MANAGER);
    }

    private static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }
}
