package edu.seu.vcampus.server.dorm.ext.schedule;

import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.server.dorm.service.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import org.threeten.bp.LocalDateTime;

/** Single-threaded, one-shot scheduler for the five dorm background jobs. */
public final class DormScheduler implements DormTaskRunner {
    public static final String KEY_ENABLED = "vcampus.dorm.scheduler.enabled";
    public static final String KEY_OPERATOR = "vcampus.dorm.scheduler.operator";
    private static final Logger LOGGER = Logger.getLogger(DormScheduler.class.getName());
    private final DormExtService service;
    private final DormSchedulerStatus status;
    private final List<Job> jobs = new ArrayList<Job>();
    private final ScheduledExecutorService executor;
    private volatile boolean stopped;

    public DormScheduler(DormExtService service) { this(service, operatorFromProperties()); }
    public DormScheduler(DormExtService service, Long operator) {
        if (service == null) throw new IllegalArgumentException("service is required");
        this.service = service; status = service.schedulerStatus();
        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() { @Override public Thread newThread(Runnable r) { Thread t = new Thread(r, "vcampus-dorm-scheduler"); t.setDaemon(true); return t; } });
        for (DormSchedulerJobDefinition definition : DormSchedulerJobs.create(service, operator)) add(definition);
    }
    public static boolean enabled() { return !"false".equalsIgnoreCase(System.getProperty(KEY_ENABLED, "true").trim()); }
    private static Long operatorFromProperties() {
        String value = System.getProperty(KEY_OPERATOR); if (value == null || value.trim().isEmpty()) return null;
        try { long parsed = Long.parseLong(value.trim()); return parsed > 0L ? Long.valueOf(parsed) : null; }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(KEY_OPERATOR + " 不是有效用户号: " + value, ex); }
    }
    private void add(DormSchedulerJobDefinition definition) { status.register(definition.name, definition.schedule); jobs.add(new Job(definition)); }
    public int taskCount() { return jobs.size(); }
    public List<String> describe() { return status.describe(); }
    public void start() { for (Job job : jobs) schedule(job); status.setRunning(true); }
    public void stop() { stopped = true; status.setRunning(false); executor.shutdownNow(); }
    public void runAllNow() { runNow(null); }
    @Override public void runNow(String taskName) {
        boolean matched = false; for (Job job : jobs) if (taskName == null || job.name.equals(taskName)) { job.execute(); matched = true; }
        if (!matched) throw new DormException(DormExtCommands.INVALID_INPUT, "未知的定时任务：" + taskName);
    }
    public List<String> taskNames() { List<String> names = new ArrayList<String>(jobs.size()); for (Job job : jobs) names.add(job.name); return names; }
    private void schedule(Job job) {
        if (stopped) return;
        LocalDateTime now = LocalDateTime.now(); long delay = DormScheduleTimes.millisBetween(now, job.plan.next(now));
        try { executor.schedule(job, delay, TimeUnit.MILLISECONDS); } catch (RejectedExecutionException ex) { stopped = true; }
    }

    interface Plan { LocalDateTime next(LocalDateTime now); }
    interface Task { String run(); }
    static final class Skipped extends RuntimeException { private static final long serialVersionUID = 1L; Skipped(String message) { super(message); } }
    private final class Job implements Runnable {
        private final String name; private final Plan plan; private final Task task;
        Job(DormSchedulerJobDefinition definition) { name = definition.name; plan = definition.plan; task = definition.task; }
        @Override public void run() { execute(); schedule(this); }
        void execute() {
            try { String summary = task.run(); status.recordSuccess(name, summary); LOGGER.info(name + " 完成：" + summary); }
            catch (Skipped ex) { status.recordSkipped(name, ex.getMessage()); LOGGER.info(name + " 跳过：" + ex.getMessage()); }
            catch (DormException ex) {
                if (DormExtCommands.NO_PENDING_READING.equals(ex.getResultCode())) { status.recordSkipped(name, ex.getMessage()); LOGGER.info(name + " 跳过：" + ex.getMessage()); return; }
                status.recordFailure(name, ex.getResultCode() + " " + ex.getMessage()); LOGGER.log(Level.WARNING, name + " 失败", ex);
            } catch (RuntimeException ex) { status.recordFailure(name, String.valueOf(ex.getMessage())); LOGGER.log(Level.WARNING, name + " 失败", ex); }
        }
    }
}
