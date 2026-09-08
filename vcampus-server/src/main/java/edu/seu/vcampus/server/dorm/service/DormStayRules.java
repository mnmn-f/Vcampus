package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

/**
 * 在宿状态与晚归判定。
 *
 * <p>两者都是门禁流水的纯函数，因此都不落库：状态存一份就会和流水打架，晚归
 * 标记存一份就会在门禁时间调整后与新策略对不上。这里集中写清判定口径，
 * 让规则可以脱离数据库直接单测。</p>
 */
public final class DormStayRules {
    private DormStayRules() { }

    /**
     * 当前在宿状态。
     *
     * <p>已批准的请假优先：请了假的人不在宿舍是登记过的，不该和"擅自离宿"混在
     * 一起。其次看门禁——最后一次离宿晚于最后一次归宿即为离宿。</p>
     */
    public static String status(LocalDateTime lastExitAt, LocalDateTime lastEntryAt,
                               boolean onApprovedLeave) {
        if (onApprovedLeave) return StayStatusDto.LEAVE_REGISTERED;
        if (lastExitAt == null) return StayStatusDto.IN_DORM;
        if (lastEntryAt != null && !lastEntryAt.isBefore(lastExitAt)) return StayStatusDto.IN_DORM;
        return StayStatusDto.OUT;
    }

    /**
     * 是否算晚归。
     *
     * <p>只有归宿记录可能晚归。晚于门禁时间是常规晚归；早于清晨时间同样算——
     * 凌晨两点回来在时钟上"早"，在管理上是整夜未归，只看"晚于 23:00"会把这类
     * 漏掉。</p>
     */
    public static boolean isLateReturn(String recordType, LocalDateTime occurredAt,
                                       LocalTime curfew, LocalTime dawn) {
        if (!"ENTRY".equalsIgnoreCase(recordType) || occurredAt == null) return false;
        if (curfew == null || dawn == null) return false;
        LocalTime time = occurredAt.toLocalTime();
        return !time.isBefore(curfew) || time.isBefore(dawn);
    }
}
