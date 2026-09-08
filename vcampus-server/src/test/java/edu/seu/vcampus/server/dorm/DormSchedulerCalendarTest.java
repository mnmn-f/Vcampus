package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.server.dorm.ext.schedule.DormScheduleTimes;
import edu.seu.vcampus.server.dorm.service.DormSchedulerStatus;
import org.junit.Test;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 宿舍调度时间计算与运行台账。 */
public final class DormSchedulerCalendarTest {
    private static final LocalTime AT_0800 = LocalTime.of(8, 0);

    @Test public void dailyTriggerRollsCorrectly() {
        assertEquals(LocalDateTime.of(2026, 9, 1, 8, 0),
                DormScheduleTimes.nextDaily(LocalDateTime.of(2026, 9, 1, 7, 30), AT_0800));
        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 0),
                DormScheduleTimes.nextDaily(LocalDateTime.of(2026, 9, 1, 8, 30), AT_0800));
        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 0),
                DormScheduleTimes.nextDaily(LocalDateTime.of(2026, 9, 1, 8, 0), AT_0800));
        assertEquals(LocalDateTime.of(2026, 10, 1, 8, 0),
                DormScheduleTimes.nextDaily(LocalDateTime.of(2026, 9, 30, 9, 0), AT_0800));
    }
    @Test public void weeklyTriggerSelectsNextMatchingTime() {
        assertEquals(LocalDateTime.of(2026, 9, 7, 9, 0), DormScheduleTimes.nextWeekly(
                LocalDateTime.of(2026, 9, 1, 10, 0), DayOfWeek.MONDAY, LocalTime.of(9, 0)));
        assertEquals(LocalDateTime.of(2026, 9, 7, 9, 0), DormScheduleTimes.nextWeekly(
                LocalDateTime.of(2026, 9, 7, 8, 0), DayOfWeek.MONDAY, LocalTime.of(9, 0)));
        assertEquals(LocalDateTime.of(2026, 9, 14, 9, 0), DormScheduleTimes.nextWeekly(
                LocalDateTime.of(2026, 9, 7, 9, 30), DayOfWeek.MONDAY, LocalTime.of(9, 0)));
    }
    @Test public void monthlyTriggerClampsAndCrossesBoundaries() {
        assertEquals(LocalDateTime.of(2026, 10, 1, 9, 0), DormScheduleTimes.nextMonthly(
                LocalDateTime.of(2026, 9, 15, 10, 0), 1, LocalTime.of(9, 0)));
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0), DormScheduleTimes.nextMonthly(
                LocalDateTime.of(2026, 9, 1, 8, 0), 1, LocalTime.of(9, 0)));
        assertEquals(LocalDateTime.of(2027, 1, 1, 9, 0), DormScheduleTimes.nextMonthly(
                LocalDateTime.of(2026, 12, 20, 10, 0), 1, LocalTime.of(9, 0)));
        assertEquals(LocalDateTime.of(2027, 2, 28, 9, 0), DormScheduleTimes.nextMonthly(
                LocalDateTime.of(2027, 2, 5, 10, 0), 31, LocalTime.of(9, 0)));
    }
    @Test public void durationIsForwardOnlyAndDayMustBeValid() {
        assertEquals(90000L, DormScheduleTimes.millisBetween(LocalDateTime.of(2026, 9, 1, 8, 0, 0),
                LocalDateTime.of(2026, 9, 1, 8, 1, 30)));
        assertEquals(0L, DormScheduleTimes.millisBetween(LocalDateTime.of(2026, 9, 1, 8, 0),
                LocalDateTime.of(2026, 9, 1, 7, 0)));
        try { DormScheduleTimes.nextMonthly(LocalDateTime.now(), 0, AT_0800); fail("invalid day"); }
        catch (IllegalArgumentException expected) { assertNotNull(expected.getMessage()); }
    }
    @Test public void statusTracksRegistrationAndResults() {
        DormSchedulerStatus s = new DormSchedulerStatus(); s.register("demo", "每日 08:00");
        assertEquals(1, s.taskCount()); assertTrue(s.describe().get(0).contains("尚未执行")); assertFalse(s.isRunning());
        s.register("demo", "每日 09:00"); assertEquals(1, s.taskCount()); assertTrue(s.describe().get(0).contains("每日 09:00"));
        s.recordSuccess("demo", "扫描 3 人"); s.recordFailure("demo", "数据库不可用");
        assertEquals(2, s.runCount("demo")); assertEquals(1, s.failureCount("demo"));
        assertEquals("数据库不可用", s.lastResult("demo")); assertTrue(s.describe().get(0).contains("失败"));
    }
    @Test public void skippedAndUnknownStatusEventsAreHandled() {
        DormSchedulerStatus s = new DormSchedulerStatus(); s.register("demo", "每月 1 日 09:00");
        s.recordSkipped("demo", "未配置记账人"); assertEquals(0, s.runCount("demo")); assertEquals(0, s.failureCount("demo"));
        assertTrue(s.lastResult("demo").startsWith("跳过："));
        try { s.recordSuccess("nope", "x"); fail("unknown task"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("nope")); }
    }
}
