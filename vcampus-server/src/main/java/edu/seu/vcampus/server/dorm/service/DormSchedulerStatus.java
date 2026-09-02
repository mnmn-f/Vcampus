package edu.seu.vcampus.server.dorm.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

/**
 * 定时任务的运行状态台账。
 *
 * <p>调度器只负责「什么时候跑」，跑完的结果统一写进这里；{@code DormExtService.status()}
 * 再把它读出来返回给客户端，对应设计文档的服务端服务监控界面。</p>
 *
 * <p>调度线程写、命令线程读，因此全部读写都加同一把锁；任务条目按注册顺序排列，
 * 保证监控界面每次刷新看到的顺序稳定。</p>
 */
public final class DormSchedulerStatus {
    /** 每日未归扫描。 */
    public static final String TASK_ABSENCE_SCAN = "absenceScan";
    /** 每日严重未归汇总提醒。 */
    public static final String TASK_WARNING_NOTIFY = "warningNotify";
    /** 每周卫生检查任务生成。 */
    public static final String TASK_HYGIENE_TASK = "hygieneTaskGenerate";
    /** 每日过期公告下架。 */
    public static final String TASK_NOTICE_EXPIRE = "noticeExpireScan";
    /** 每月水电账单出账。 */
    public static final String TASK_BILL_GENERATE = "billGenerate";

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();
    private boolean running;

    /** 注册一个任务并写明它的调度周期；重复注册按更新处理，不会产生重复条目。 */
    public synchronized void register(String taskName, String schedule) {
        if (taskName == null || taskName.trim().isEmpty()) {
            throw new IllegalArgumentException("taskName is required");
        }
        Entry entry = entries.get(taskName);
        if (entry == null) {
            entry = new Entry(taskName);
            entries.put(taskName, entry);
        }
        entry.schedule = schedule == null ? "" : schedule;
    }

    public synchronized void setRunning(boolean value) { this.running = value; }

    public synchronized boolean isRunning() { return running; }

    /** 记录一次成功执行；summary 是给人看的一句话结果。 */
    public synchronized void recordSuccess(String taskName, String summary) {
        Entry entry = require(taskName);
        entry.runCount++;
        entry.lastRunAt = LocalDateTime.now();
        entry.lastSucceeded = true;
        entry.lastResult = summary == null ? "完成" : summary;
    }

    /** 记录一次失败执行；调度器捕获异常后调用，任务本身不会因此停止。 */
    public synchronized void recordFailure(String taskName, String message) {
        Entry entry = require(taskName);
        entry.runCount++;
        entry.failureCount++;
        entry.lastRunAt = LocalDateTime.now();
        entry.lastSucceeded = false;
        entry.lastResult = message == null ? "执行失败" : message;
    }

    /** 记录一次按配置跳过；跳过不计入失败，但要在监控里看得见原因。 */
    public synchronized void recordSkipped(String taskName, String reason) {
        Entry entry = require(taskName);
        entry.lastRunAt = LocalDateTime.now();
        entry.lastSucceeded = true;
        entry.lastResult = "跳过：" + (reason == null ? "未配置" : reason);
    }

    /** 已注册的任务数，供启动日志与单测确认接线完整。 */
    public synchronized int taskCount() { return entries.size(); }

    public synchronized int runCount(String taskName) { return require(taskName).runCount; }

    public synchronized int failureCount(String taskName) { return require(taskName).failureCount; }

    /** 某个任务的最近一次结果描述；从未执行过返回 null。 */
    public synchronized String lastResult(String taskName) { return require(taskName).lastResult; }

    /**
     * 监控视图：每个任务一行，形如
     * {@code absenceScan | 每日 08:00 | 最近 2026-09-01 08:00:00 成功 扫描 12 人 | 累计 3 次/失败 0 次}。
     */
    public synchronized List<String> describe() {
        List<String> lines = new ArrayList<String>(entries.size());
        for (Entry entry : entries.values()) {
            lines.add(entry.describe());
        }
        return lines;
    }

    private Entry require(String taskName) {
        Entry entry = entries.get(taskName);
        if (entry == null) {
            throw new IllegalArgumentException("未注册的定时任务: " + taskName);
        }
        return entry;
    }

    /** 单个任务的运行台账；只在外层同步块里访问，自身不再加锁。 */
    private static final class Entry {
        private final String name;
        private String schedule = "";
        private LocalDateTime lastRunAt;
        private String lastResult;
        private boolean lastSucceeded = true;
        private int runCount;
        private int failureCount;

        Entry(String name) { this.name = name; }

        String describe() {
            StringBuilder text = new StringBuilder();
            text.append(name).append(" | ").append(schedule).append(" | ");
            if (lastRunAt == null) {
                text.append("尚未执行");
            } else {
                text.append("最近 ").append(STAMP.format(lastRunAt))
                        .append(lastSucceeded ? " 成功 " : " 失败 ")
                        .append(lastResult == null ? "" : lastResult);
            }
            text.append(" | 累计 ").append(runCount).append(" 次/失败 ")
                    .append(failureCount).append(" 次");
            return text.toString();
        }
    }
}
