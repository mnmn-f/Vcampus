package edu.seu.vcampus.server.dorm.ext.schedule;

import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneTaskGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.WarningScanResultDto;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.dorm.service.DormSchedulerStatus;
import edu.seu.vcampus.server.dorm.service.DormTaskRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/**
 * 宿舍模块的定时任务调度器，对应设计文档第 9 章表 15 的五项后台任务。
 *
 * <p>每个任务都是「算出下一次触发时刻 → 单次定时 → 跑完再排下一次」，不用
 * {@code scheduleAtFixedRate}：固定频率无法表达「每周一」「每月 1 日」，而且一次
 * 执行超时会让后续批次挤在一起。单次排期还有个好处——任务体抛异常不会像
 * 固定频率那样把这条调度线永久停掉。</p>
 *
 * <p>任何一个任务失败都只记进 {@link DormSchedulerStatus} 并继续排下一次，绝不
 * 影响 TCP 服务本身；调度器整体也可以用
 * {@code -Dvcampus.dorm.scheduler.enabled=false} 关掉，此时服务端退化成纯命令
 * 响应模式，和 P5 的行为完全一致。</p>
 */
public final class DormScheduler implements DormTaskRunner {
    /** 关掉调度器只跑命令服务。 */
    public static final String KEY_ENABLED = "vcampus.dorm.scheduler.enabled";
    /** 月度出账的记账人用户号；不配置就跳过出账任务。 */
    public static final String KEY_OPERATOR = "vcampus.dorm.scheduler.operator";

    private static final LocalTime ABSENCE_AT = LocalTime.of(8, 0);
    private static final LocalTime NOTIFY_AT = LocalTime.of(8, 10);
    private static final LocalTime HYGIENE_AT = LocalTime.of(9, 0);
    private static final LocalTime NOTICE_AT = LocalTime.of(0, 0);
    private static final LocalTime BILL_AT = LocalTime.of(9, 0);
    private static final int BILL_DAY_OF_MONTH = 1;

    private final DormExtService service;
    private final DormSchedulerStatus status;
    private final Long billOperatorUserId;
    private final List<Job> jobs = new ArrayList<Job>();
    private final ScheduledExecutorService executor;
    private volatile boolean stopped;

    public DormScheduler(DormExtService service) {
        this(service, operatorFromProperties());
    }

    public DormScheduler(DormExtService service, Long billOperatorUserId) {
        if (service == null) {
            throw new IllegalArgumentException("service is required");
        }
        this.service = service;
        this.status = service.schedulerStatus();
        this.billOperatorUserId = billOperatorUserId;
        this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "vcampus-dorm-scheduler");
                // 守护线程：即使忘了调 stop()，JVM 也不会被调度器吊着不退出。
                thread.setDaemon(true);
                return thread;
            }
        });
        defineJobs();
    }

    /** 是否启用调度器；默认启用。 */
    public static boolean enabled() {
        return !"false".equalsIgnoreCase(
                System.getProperty(KEY_ENABLED, "true").trim());
    }

    private static Long operatorFromProperties() {
        String value = System.getProperty(KEY_OPERATOR);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0L ? Long.valueOf(parsed) : null;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(KEY_OPERATOR + " 不是有效用户号: " + value, ex);
        }
    }

    private void defineJobs() {
        add(DormSchedulerStatus.TASK_ABSENCE_SCAN, "每日 08:00", new Plan() {
            @Override
            public LocalDateTime next(LocalDateTime now) {
                return DormScheduleTimes.nextDaily(now, ABSENCE_AT);
            }
        }, new Task() {
            @Override
            public String run() {
                WarningScanResultDto result = service.runAbsenceScan(LocalDate.now());
                return "扫描 " + result.getResidentsScanned() + " 人，预警 "
                        + result.getWarningCount() + " 条（一般 " + result.getNormalCount()
                        + "/严重 " + result.getSevereCount() + "/免除 "
                        + result.getExemptCount() + "）";
            }
        });

        add(DormSchedulerStatus.TASK_WARNING_NOTIFY, "每日 08:10", new Plan() {
            @Override
            public LocalDateTime next(LocalDateTime now) {
                return DormScheduleTimes.nextDaily(now, NOTIFY_AT);
            }
        }, new Task() {
            @Override
            public String run() {
                List<AbsenceWarningDto> pending = service.pendingSevereWarnings(LocalDate.now());
                if (pending.isEmpty()) {
                    return "无待通知的严重未归预警";
                }
                // 学生到辅导员的映射不在 main 的数据模型里，服务端无法自动定位收件人，
                // 因此这里只汇总待办并落到监控台账，通知动作仍由宿管手动确认收件人。
                StringBuilder text = new StringBuilder();
                text.append("待通知严重未归 ").append(pending.size()).append(" 条：");
                int shown = 0;
                for (AbsenceWarningDto item : pending) {
                    if (shown > 0) text.append('、');
                    text.append(item.getBuildingCode()).append(item.getRoomNo())
                            .append('/').append(item.getStudentUserId())
                            .append('(').append(item.getAbsenceDays()).append("天)");
                    if (++shown >= 5) break;
                }
                if (pending.size() > shown) {
                    text.append(" 等");
                }
                return text.toString();
            }
        });

        add(DormSchedulerStatus.TASK_HYGIENE_TASK, "每周一 09:00", new Plan() {
            @Override
            public LocalDateTime next(LocalDateTime now) {
                return DormScheduleTimes.nextWeekly(now, DayOfWeek.MONDAY, HYGIENE_AT);
            }
        }, new Task() {
            @Override
            public String run() {
                HygieneTaskGenerateResultDto result = service.runHygieneTaskGenerate(
                        new HygieneTaskGenerateRequest(null, LocalDate.now()));
                return "覆盖 " + result.getRoomsScanned() + " 间，新建 " + result.getCreated()
                        + " 条，已存在 " + result.getExisting() + " 条";
            }
        });

        add(DormSchedulerStatus.TASK_NOTICE_EXPIRE, "每日 00:00", new Plan() {
            @Override
            public LocalDateTime next(LocalDateTime now) {
                return DormScheduleTimes.nextDaily(now, NOTICE_AT);
            }
        }, new Task() {
            @Override
            public String run() {
                return "下架过期公告 " + service.runNoticeExpireScan(LocalDateTime.now()) + " 条";
            }
        });

        add(DormSchedulerStatus.TASK_BILL_GENERATE, "每月 1 日 09:00", new Plan() {
            @Override
            public LocalDateTime next(LocalDateTime now) {
                return DormScheduleTimes.nextMonthly(now, BILL_DAY_OF_MONTH, BILL_AT);
            }
        }, new Task() {
            @Override
            public String run() {
                if (billOperatorUserId == null) {
                    throw new Skipped("未配置 " + KEY_OPERATOR + "，无法确定记账人");
                }
                LocalDate firstOfThisMonth = LocalDate.now().withDayOfMonth(1);
                LocalDate periodStart = firstOfThisMonth.minusMonths(1);
                LocalDate periodEnd = firstOfThisMonth.minusDays(1);
                BillGenerateResultDto result = service.runBillGenerate(
                        new BillGenerateRequest(null, periodStart, periodEnd, null),
                        billOperatorUserId.longValue());
                return periodStart + " 至 " + periodEnd + " 出账 " + result.getBillCount()
                        + " 张，分摊 " + result.getAllocationCount() + " 条，合计 "
                        + result.getTotalAmount() + " 元，跳过 " + result.getSkippedCount() + " 间";
            }
        });
    }

    private void add(String name, String schedule, Plan plan, Task task) {
        status.register(name, schedule);
        jobs.add(new Job(name, plan, task));
    }

    /** 已登记的任务数量，启动日志用它确认接线完整。 */
    public int taskCount() { return jobs.size(); }

    /** 每个任务的调度周期描述，按登记顺序。 */
    public List<String> describe() { return status.describe(); }

    /** 排入首次执行；重复调用无副作用之外的意义，请只在启动时调一次。 */
    public void start() {
        for (Job job : jobs) {
            schedule(job);
        }
        status.setRunning(true);
    }

    /** 停止调度；正在执行的任务跑完即止，不再排下一次。 */
    public void stop() {
        stopped = true;
        status.setRunning(false);
        executor.shutdownNow();
    }

    /**
     * 在当前线程里立刻把每个任务各跑一次，不排期。
     *
     * <p>定时任务最容易出的问题是「到点了才发现根本跑不通」，这个入口让链路可以在
     * 不等到点的情况下被验证。</p>
     */
    public void runAllNow() {
        runNow(null);
    }

    @Override
    public void runNow(String taskName) {
        boolean matched = false;
        for (Job job : jobs) {
            if (taskName == null || job.name.equals(taskName)) {
                job.execute();
                matched = true;
            }
        }
        if (!matched) {
            throw new DormException(DormExtCommands.INVALID_INPUT, "未知的定时任务：" + taskName);
        }
    }

    /** 已登记的任务名，按声明顺序；客户端用它填触发下拉。 */
    public List<String> taskNames() {
        List<String> names = new ArrayList<String>(jobs.size());
        for (Job job : jobs) {
            names.add(job.name);
        }
        return names;
    }

    private void schedule(Job job) {
        if (stopped) return;
        LocalDateTime now = LocalDateTime.now();
        long delay = DormScheduleTimes.millisBetween(now, job.plan.next(now));
        try {
            executor.schedule(job, delay, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            // stop() 与排期竞态时会走到这里，属于正常关停路径，不需要报警。
            stopped = true;
        }
    }

    private static void say(String text) {
        System.out.println("[dorm-ext] [scheduler] " + text);
    }

    /** 任务体主动跳过本次执行；不是错误，不计入失败次数。 */
    private static final class Skipped extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Skipped(String message) { super(message); }
    }

    private interface Plan {
        LocalDateTime next(LocalDateTime now);
    }

    private interface Task {
        String run();
    }

    private final class Job implements Runnable {
        private final String name;
        private final Plan plan;
        private final Task task;

        Job(String name, Plan plan, Task task) {
            this.name = name;
            this.plan = plan;
            this.task = task;
        }

        @Override
        public void run() {
            execute();
            schedule(this);
        }

        /** 执行一次并写台账；任何异常都在这里收口，绝不让它逃到调度线程外。 */
        void execute() {
            try {
                String summary = task.run();
                status.recordSuccess(name, summary);
                say(name + " 完成：" + summary);
            } catch (Skipped skipped) {
                status.recordSkipped(name, skipped.getMessage());
                say(name + " 跳过：" + skipped.getMessage());
            } catch (DormException ex) {
                if (DormExtCommands.NO_PENDING_READING.equals(ex.getResultCode())) {
                    // 没有待出账读数是月度出账的常态（宿管当月没抄表），不是故障。
                    status.recordSkipped(name, ex.getMessage());
                    say(name + " 跳过：" + ex.getMessage());
                    return;
                }
                status.recordFailure(name, ex.getResultCode() + " " + ex.getMessage());
                say(name + " 失败：" + ex.getResultCode() + " " + ex.getMessage());
            } catch (RuntimeException ex) {
                status.recordFailure(name, String.valueOf(ex.getMessage()));
                say(name + " 失败：" + ex);
                ex.printStackTrace();
            }
        }
    }
}
