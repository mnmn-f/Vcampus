package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.ext.AbsenceWarningDto;
import edu.seu.vcampus.server.dorm.service.DormAbsenceRules;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertEquals;

/** 连续未归的日期计算与分级规则。 */
public final class DormAbsenceRulesTest {
    private static final LocalDateTime LEFT = LocalDateTime.of(2026, 9, 1, 20, 0);

    @Test public void absenceCountsFromLastExit() {
        assertEquals(4, DormAbsenceRules.absenceDays(LEFT, null, LocalDate.of(2026, 9, 5)));
    }
    @Test public void returningHomeResetsAbsence() {
        assertEquals(0, DormAbsenceRules.absenceDays(LEFT,
                LocalDateTime.of(2026, 9, 2, 8, 0), LocalDate.of(2026, 9, 5)));
    }
    @Test public void sameDayExitAndNoHistoryAreNotAbsence() {
        assertEquals(0, DormAbsenceRules.absenceDays(LEFT, null, LocalDate.of(2026, 9, 1)));
        assertEquals(0, DormAbsenceRules.absenceDays(null, null, LocalDate.of(2026, 9, 5)));
    }
    @Test public void approvedLeaveOutranksDayCount() {
        assertEquals(AbsenceWarningDto.LEVEL_EXEMPT, DormAbsenceRules.level(30, 7, true));
        assertEquals(AbsenceWarningDto.LEVEL_SEVERE, DormAbsenceRules.level(30, 7, false));
        assertEquals(AbsenceWarningDto.LEVEL_NORMAL, DormAbsenceRules.level(4, 7, false));
    }
}
