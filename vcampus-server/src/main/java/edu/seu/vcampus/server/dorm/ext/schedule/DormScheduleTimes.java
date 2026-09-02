package edu.seu.vcampus.server.dorm.ext.schedule;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Duration;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/**
 * 下一次触发时刻的计算。
 *
 * <p>抽成纯函数是为了能直接单测：调度器本身只负责把这里算出来的毫秒数交给
 * {@code ScheduledExecutorService}，跨月、跨年、月末不足日这些边界都在这里定死。</p>
 *
 * <p>所有方法都返回严格晚于 {@code now} 的时刻，因此任务执行完重新排期时不会
 * 因为「当前时刻正好等于计划时刻」而在同一秒内连跑两次。</p>
 */
public final class DormScheduleTimes {
    private DormScheduleTimes() { }

    /** 每天固定时刻；今天的该时刻已过就顺延到明天。 */
    public static LocalDateTime nextDaily(LocalDateTime now, LocalTime at) {
        check(now, at);
        LocalDateTime candidate = now.toLocalDate().atTime(at);
        return candidate.isAfter(now) ? candidate : candidate.plusDays(1);
    }

    /** 每周固定星期几的固定时刻。 */
    public static LocalDateTime nextWeekly(LocalDateTime now, DayOfWeek day, LocalTime at) {
        check(now, at);
        if (day == null) {
            throw new IllegalArgumentException("day is required");
        }
        LocalDateTime candidate = now.toLocalDate().atTime(at);
        // 最多推 7 天必然命中：先对齐星期几，再保证严格晚于当前时刻。
        for (int i = 0; i < 8; i++) {
            if (candidate.getDayOfWeek() == day && candidate.isAfter(now)) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        throw new IllegalStateException("无法计算下一次执行时间");
    }

    /**
     * 每月固定日期的固定时刻。
     *
     * <p>{@code dayOfMonth} 超过当月天数时落到当月最后一天，因此 31 号的月度任务
     * 在 2 月会在 28/29 号执行，而不是被整月跳过。</p>
     */
    public static LocalDateTime nextMonthly(LocalDateTime now, int dayOfMonth, LocalTime at) {
        check(now, at);
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("dayOfMonth 必须在 1 到 31 之间");
        }
        LocalDateTime candidate = inMonth(now.toLocalDate(), dayOfMonth, at);
        if (candidate.isAfter(now)) {
            return candidate;
        }
        return inMonth(now.toLocalDate().withDayOfMonth(1).plusMonths(1), dayOfMonth, at);
    }

    /** 从 from 到 to 的毫秒数；to 不晚于 from 时返回 0，交给调度器立即执行。 */
    public static long millisBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from/to are required");
        }
        long millis = Duration.between(from, to).toMillis();
        return millis < 0L ? 0L : millis;
    }

    private static LocalDateTime inMonth(LocalDate anyDayInMonth, int dayOfMonth, LocalTime at) {
        int day = Math.min(dayOfMonth, anyDayInMonth.lengthOfMonth());
        return anyDayInMonth.withDayOfMonth(day).atTime(at);
    }

    private static void check(LocalDateTime now, LocalTime at) {
        if (now == null || at == null) {
            throw new IllegalArgumentException("now/at are required");
        }
    }
}
