package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.temporal.ChronoUnit;

/**
 * 未归判定与分级的纯函数。
 *
 * <p>抽成独立类而不是塞进服务里，是为了让「多少天算未归、几天升为严重、
 * 请假怎么豁免」这些规则可以脱离数据库直接单测——这正是设计文档里
 * absenceScan 与 warningGenerate 的判定核心。</p>
 */
public final class DormAbsenceRules {
    private DormAbsenceRules() { }

    /**
     * 连续未归天数。
     *
     * <p>判定依据是门禁流水：只有当最后一次离宿晚于最后一次归宿时，学生才处于
     * 离宿状态；此时未归天数按「最后离宿那天」到扫描日的自然日差计算。当天离宿
     * 当天扫描记 0 天，不算未归。</p>
     */
    public static int absenceDays(LocalDateTime lastExitAt, LocalDateTime lastEntryAt,
                                  LocalDate scanDate) {
        if (lastExitAt == null || scanDate == null) return 0;
        if (lastEntryAt != null && !lastEntryAt.isBefore(lastExitAt)) return 0;
        long days = ChronoUnit.DAYS.between(lastExitAt.toLocalDate(), scanDate);
        return days <= 0L ? 0 : (int) days;
    }

    /** 是否达到生成预警的门槛。 */
    public static boolean shouldWarn(int absenceDays, int warnDays) {
        return warnDays > 0 && absenceDays >= warnDays;
    }

    /**
     * 预警级别。
     *
     * <p>已批准的请假优先于天数：请了假的长期不归是正常的，不应该按严重预警
     * 惊动辅导员，但仍然留一条已豁免记录备查。</p>
     */
    public static String level(int absenceDays, int notifyDays, boolean onApprovedLeave) {
        if (onApprovedLeave) return AbsenceWarningDto.LEVEL_EXEMPT;
        return absenceDays >= notifyDays
                ? AbsenceWarningDto.LEVEL_SEVERE
                : AbsenceWarningDto.LEVEL_NORMAL;
    }
}
