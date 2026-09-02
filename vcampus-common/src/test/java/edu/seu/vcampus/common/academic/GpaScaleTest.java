package edu.seu.vcampus.common.academic;

import java.math.BigDecimal;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** 东南大学4.8制区间边界和统一舍入测试。 */
public final class GpaScaleTest {
    @Test
    public void mapsOfficialBoundaryScores() {
        assertEquals(new BigDecimal("4.8"), GpaScale.point(new BigDecimal("96")));
        assertEquals(new BigDecimal("4.5"), GpaScale.point(new BigDecimal("93")));
        assertEquals(new BigDecimal("4.0"), GpaScale.point(new BigDecimal("90")));
        assertEquals(new BigDecimal("3.8"), GpaScale.point(new BigDecimal("86")));
        assertEquals(new BigDecimal("3.5"), GpaScale.point(new BigDecimal("83")));
        assertEquals(new BigDecimal("3.0"), GpaScale.point(new BigDecimal("80")));
        assertEquals(new BigDecimal("2.8"), GpaScale.point(new BigDecimal("76")));
        assertEquals(new BigDecimal("2.5"), GpaScale.point(new BigDecimal("73")));
        assertEquals(new BigDecimal("2.0"), GpaScale.point(new BigDecimal("70")));
        assertEquals(new BigDecimal("1.8"), GpaScale.point(new BigDecimal("66")));
        assertEquals(new BigDecimal("1.5"), GpaScale.point(new BigDecimal("63")));
        assertEquals(new BigDecimal("1.0"), GpaScale.point(new BigDecimal("60")));
        assertEquals(new BigDecimal("0.0"), GpaScale.point(new BigDecimal("59.99")));
    }

    @Test
    public void roundsHalfUpToTwoDecimals() {
        assertEquals(new BigDecimal("2.35"), GpaScale.round(new BigDecimal("2.345")));
    }
}
