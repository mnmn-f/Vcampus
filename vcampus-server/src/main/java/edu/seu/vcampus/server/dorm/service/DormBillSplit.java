package edu.seu.vcampus.server.dorm.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 房间账单在室友之间的均摊算法。
 *
 * <p>分摊结果必须精确等于房间总额：先按分向下取整均分，再把不足一分的余数
 * 逐分补给排在前面的人，因此不会出现「四舍五入后合计对不上总额」的账。
 * {@code utility_allocations} 有 {@code amount > 0} 的 CHECK 约束，所以总额
 * 不足以让每人分到一分时不允许出账，由调用方跳过并记录原因。</p>
 */
public final class DormBillSplit {
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private DormBillSplit() { }

    /** 总额能否让每个人都至少分到一分钱。 */
    public static boolean isSplittable(BigDecimal total, int shares) {
        if (total == null || shares <= 0) return false;
        return total.compareTo(CENT.multiply(BigDecimal.valueOf(shares))) >= 0;
    }

    /** 返回长度为 shares 的分摊金额，合计精确等于 total。 */
    public static List<BigDecimal> split(BigDecimal total, int shares) {
        if (!isSplittable(total, shares)) {
            throw new IllegalArgumentException("amount is too small to split");
        }
        BigDecimal amount = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = amount.divide(BigDecimal.valueOf(shares), 2, RoundingMode.DOWN);
        BigDecimal distributed = base.multiply(BigDecimal.valueOf(shares));
        long extraCents = amount.subtract(distributed).movePointRight(2).longValue();
        List<BigDecimal> result = new ArrayList<BigDecimal>(shares);
        for (int i = 0; i < shares; i++) {
            result.add(i < extraCents ? base.add(CENT) : base);
        }
        return result;
    }
}
