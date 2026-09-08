package edu.seu.vcampus.server.dorm.ext.schedule;

import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.dorm.service.DormSchedulerStatus;
import edu.seu.vcampus.server.dorm.service.DormTaskRunner;
import java.util.*;
import org.threeten.bp.*;

/** Builds the five documented dorm background jobs without bloating the scheduler. */
final class DormSchedulerJobs {
    private static final LocalTime ABSENCE = LocalTime.of(8, 0);
    private static final LocalTime NOTIFY = LocalTime.of(8, 10);
    private static final LocalTime HYGIENE = LocalTime.of(9, 0);
    private static final LocalTime NOTICE = LocalTime.MIDNIGHT;
    private static final LocalTime BILL = LocalTime.of(9, 0);
    private DormSchedulerJobs() { }

    static List<DormSchedulerJobDefinition> create(final DormExtService service, final Long operator) {
        List<DormSchedulerJobDefinition> jobs = new ArrayList<DormSchedulerJobDefinition>();
        jobs.add(job(DormSchedulerStatus.TASK_ABSENCE_SCAN, "每日 08:00", new DormScheduler.Plan() { public LocalDateTime next(LocalDateTime now) { return DormScheduleTimes.nextDaily(now, ABSENCE); } }, new DormScheduler.Task() { public String run() { WarningScanResultDto r = service.runAbsenceScan(LocalDate.now()); return "扫描 " + r.getResidentsScanned() + " 人，预警 " + r.getWarningCount() + " 条（一般 " + r.getNormalCount() + "/严重 " + r.getSevereCount() + "/免除 " + r.getExemptCount() + "）"; } }));
        jobs.add(job(DormSchedulerStatus.TASK_WARNING_NOTIFY, "每日 08:10", new DormScheduler.Plan() { public LocalDateTime next(LocalDateTime now) { return DormScheduleTimes.nextDaily(now, NOTIFY); } }, new DormScheduler.Task() { public String run() { return warningSummary(service.pendingSevereWarnings(LocalDate.now())); } }));
        jobs.add(job(DormSchedulerStatus.TASK_HYGIENE_TASK, "每周一 09:00", new DormScheduler.Plan() { public LocalDateTime next(LocalDateTime now) { return DormScheduleTimes.nextWeekly(now, DayOfWeek.MONDAY, HYGIENE); } }, new DormScheduler.Task() { public String run() { HygieneTaskGenerateResultDto r = service.runHygieneTaskGenerate(new HygieneTaskGenerateRequest(null, LocalDate.now())); return "覆盖 " + r.getRoomsScanned() + " 间，新建 " + r.getCreated() + " 条，已存在 " + r.getExisting() + " 条"; } }));
        jobs.add(job(DormSchedulerStatus.TASK_NOTICE_EXPIRE, "每日 00:00", new DormScheduler.Plan() { public LocalDateTime next(LocalDateTime now) { return DormScheduleTimes.nextDaily(now, NOTICE); } }, new DormScheduler.Task() { public String run() { return "下架过期公告 " + service.runNoticeExpireScan(LocalDateTime.now()) + " 条"; } }));
        jobs.add(job(DormSchedulerStatus.TASK_BILL_GENERATE, "每月 1 日 09:00", new DormScheduler.Plan() { public LocalDateTime next(LocalDateTime now) { return DormScheduleTimes.nextMonthly(now, 1, BILL); } }, new DormScheduler.Task() { public String run() { if (operator == null) throw new DormScheduler.Skipped("未配置 " + DormScheduler.KEY_OPERATOR + "，无法确定记账人"); LocalDate first = LocalDate.now().withDayOfMonth(1); LocalDate start = first.minusMonths(1); LocalDate end = first.minusDays(1); BillGenerateResultDto r = service.runBillGenerate(new BillGenerateRequest(null, start, end, null), operator.longValue()); return start + " 至 " + end + " 出账 " + r.getBillCount() + " 张，分摊 " + r.getAllocationCount() + " 条，合计 " + r.getTotalAmount() + " 元，跳过 " + r.getSkippedCount() + " 间"; } }));
        return jobs;
    }
    private static DormSchedulerJobDefinition job(String name, String schedule, DormScheduler.Plan plan, DormScheduler.Task task) { return new DormSchedulerJobDefinition(name, schedule, plan, task); }
    private static String warningSummary(List<AbsenceWarningDto> pending) {
        if (pending.isEmpty()) return "无待通知的严重未归预警";
        StringBuilder text = new StringBuilder("待通知严重未归 ").append(pending.size()).append(" 条："); int shown = 0;
        for (AbsenceWarningDto item : pending) { if (shown > 0) text.append('、'); text.append(item.getBuildingCode()).append(item.getRoomNo()).append('/').append(item.getStudentUserId()).append('(').append(item.getAbsenceDays()).append("天)"); if (++shown >= 5) break; }
        if (pending.size() > shown) text.append(" 等"); return text.toString();
    }
}
