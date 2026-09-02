package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.ext.HygieneDetailDto;
import edu.seu.vcampus.common.dto.dorm.ext.HygieneItemScoreDto;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.threeten.bp.LocalDate;

/**
 * 卫生检查的评分、定级与整改判定。
 *
 * <p>抽成纯函数是为了让「怎么算总分、多少分算优秀、几分触发整改、复查排在哪天」
 * 这几条规则可以脱离数据库直接单测；它们也是设计文档里 hygieneScoreSubmit 与
 * rectifyNoticeSend 的判定核心。</p>
 */
public final class DormHygieneRules {
    /** 总分低于这条线触发整改与复查，对应设计文档的「总分低于 70 时推送」。 */
    public static final int RECTIFY_THRESHOLD = 70;
    /** 整改后复查安排在检查日之后的天数。 */
    public static final int RECHECK_AFTER_DAYS = 3;

    private DormHygieneRules() { }

    /**
     * 校验分项是否完整合法。
     *
     * <p>要求五项齐全、不重复、每项 0~20 分：缺项会让总分凭空变低，重复项会让
     * 总分超过 100，两种情况都会把后续的定级和整改判定带偏，因此在入口就拦住。</p>
     */
    public static void validate(List<HygieneItemScoreDto> items) {
        if (items == null || items.size() != HygieneItemScoreDto.ITEM_CODES.length) {
            throw new IllegalArgumentException("必须提交全部 "
                    + HygieneItemScoreDto.ITEM_CODES.length + " 个检查项");
        }
        Set<String> seen = new HashSet<String>();
        for (HygieneItemScoreDto item : items) {
            if (item == null || item.getItemCode() == null) {
                throw new IllegalArgumentException("检查项编码不能为空");
            }
            if (!isKnown(item.getItemCode())) {
                throw new IllegalArgumentException("未知的检查项：" + item.getItemCode());
            }
            if (!seen.add(item.getItemCode())) {
                throw new IllegalArgumentException("检查项重复："
                        + HygieneItemScoreDto.itemName(item.getItemCode()));
            }
            BigDecimal score = item.getScore();
            if (score == null || score.signum() < 0
                    || score.compareTo(BigDecimal.valueOf(HygieneItemScoreDto.MAX_ITEM_SCORE)) > 0) {
                throw new IllegalArgumentException(
                        HygieneItemScoreDto.itemName(item.getItemCode())
                        + "得分必须在 0 到 " + HygieneItemScoreDto.MAX_ITEM_SCORE + " 之间");
            }
        }
    }

    /** 五项之和即为总分。 */
    public static BigDecimal total(List<HygieneItemScoreDto> items) {
        BigDecimal sum = BigDecimal.ZERO;
        for (HygieneItemScoreDto item : items) sum = sum.add(item.getScore());
        return sum;
    }

    /** 优秀 ≥90，良好 ≥80，合格 ≥60，其余为差。 */
    public static String level(BigDecimal total) {
        if (total.compareTo(BigDecimal.valueOf(90)) >= 0) return HygieneDetailDto.LEVEL_EXCELLENT;
        if (total.compareTo(BigDecimal.valueOf(80)) >= 0) return HygieneDetailDto.LEVEL_GOOD;
        if (total.compareTo(BigDecimal.valueOf(60)) >= 0) return HygieneDetailDto.LEVEL_PASS;
        return HygieneDetailDto.LEVEL_POOR;
    }

    public static boolean needRectify(BigDecimal total) {
        return total.compareTo(BigDecimal.valueOf(RECTIFY_THRESHOLD)) < 0;
    }

    /** V1 的 hygiene_inspections.result 只有 PASS/FAIL 两态，按整改线映射。 */
    public static String result(BigDecimal total) {
        return needRectify(total) ? "FAIL" : "PASS";
    }

    /** V1 的 hygiene_inspections.status。 */
    public static String status(BigDecimal total) {
        return needRectify(total) ? "RECTIFICATION_REQUIRED" : "NORMAL";
    }

    public static LocalDate recheckDate(LocalDate inspectedOn) {
        return inspectedOn == null ? null : inspectedOn.plusDays(RECHECK_AFTER_DAYS);
    }

    private static boolean isKnown(String code) {
        for (String known : HygieneItemScoreDto.ITEM_CODES) {
            if (known.equals(code)) return true;
        }
        return false;
    }
}
