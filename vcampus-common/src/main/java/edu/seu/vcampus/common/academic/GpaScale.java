package edu.seu.vcampus.common.academic;

import java.math.BigDecimal;

/** 东南大学4.8制成绩映射及统一两位小数舍入规则。 */
public final class GpaScale {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private GpaScale() {
    }

    public static BigDecimal point(BigDecimal score) {
        if (score == null) {
            return null;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        if (score.compareTo(new BigDecimal("96")) >= 0) return new BigDecimal("4.8");
        if (score.compareTo(new BigDecimal("93")) >= 0) return new BigDecimal("4.5");
        if (score.compareTo(new BigDecimal("90")) >= 0) return new BigDecimal("4.0");
        if (score.compareTo(new BigDecimal("86")) >= 0) return new BigDecimal("3.8");
        if (score.compareTo(new BigDecimal("83")) >= 0) return new BigDecimal("3.5");
        if (score.compareTo(new BigDecimal("80")) >= 0) return new BigDecimal("3.0");
        if (score.compareTo(new BigDecimal("76")) >= 0) return new BigDecimal("2.8");
        if (score.compareTo(new BigDecimal("73")) >= 0) return new BigDecimal("2.5");
        if (score.compareTo(new BigDecimal("70")) >= 0) return new BigDecimal("2.0");
        if (score.compareTo(new BigDecimal("66")) >= 0) return new BigDecimal("1.8");
        if (score.compareTo(new BigDecimal("63")) >= 0) return new BigDecimal("1.5");
        if (score.compareTo(new BigDecimal("60")) >= 0) return new BigDecimal("1.0");
        return BigDecimal.ZERO.setScale(1);
    }

    public static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
