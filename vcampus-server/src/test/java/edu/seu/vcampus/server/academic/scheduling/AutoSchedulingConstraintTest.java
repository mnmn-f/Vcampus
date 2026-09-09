package edu.seu.vcampus.server.academic.scheduling;

import edu.seu.vcampus.common.dto.academic.AutoScheduleEntryDto;
import edu.seu.vcampus.common.dto.academic.AutoSchedulePreviewDto;
import edu.seu.vcampus.common.dto.academic.TimePreferenceType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 自动排课的多教师、学生组、资源和边界约束。 */
public final class AutoSchedulingConstraintTest {
    private final AutoSchedulingSolver solver = new AutoSchedulingSolver();

    @Test public void sharedTeacherIsScheduledAtDifferentPeriods() {
        AutoSchedulingSolver.Session first = session(1, "A", Arrays.asList(10L, 11L), "G1", 30);
        AutoSchedulingSolver.Session second = session(2, "B", Collections.singletonList(11L), "G2", 30);
        AutoSchedulePreviewDto result = solve(Arrays.asList(first, second), rooms(60, 61),
                slots(1, 1, 3));
        assertTrue(result.isSuccess());
        assertFalse(overlaps(result.getEntries().get(0), result.getEntries().get(1)));
    }

    @Test public void sharedStudentGroupCannotUseSamePeriod() {
        AutoSchedulePreviewDto result = solve(Arrays.asList(session(1, "A",
                Collections.singletonList(10L), "G1", 30), session(2, "B",
                Collections.singletonList(11L), "G1", 30)), rooms(60, 61), slots(1, 1));
        assertFalse(result.isSuccess());
    }

    @Test public void unavailableTeacherAndRoomRequirementsAreHardConstraints() {
        AutoSchedulingSolver.Session session = session(1, "LAB", Collections.singletonList(10L),
                "G1", 80, "LAB");
        AutoSchedulePreviewDto result = solver.solve(problem(Collections.singletonList(session),
                Collections.singletonList(new AutoSchedulingSolver.Room(1, "教室", "TEACHING", 60)),
                Collections.<AutoSchedulingSolver.Fixed>emptyList(), Collections.singletonList(
                        new AutoSchedulingSolver.Preference(10, new AutoSchedulingSolver.TimeSlot(1, 1, 2),
                                TimePreferenceType.UNAVAILABLE)), slots(1, 1)), 1000);
        assertFalse(result.isSuccess());
        assertTrue(result.getExplanations().get(0).contains("容量"));
    }

    @Test public void validatePlanRejectsInvalidPeriodAndClassroom() {
        AutoSchedulingSolver.Session session = session(1, "A", Collections.singletonList(10L), "G1", 30);
        AutoSchedulingSolver.Problem problem = problem(Collections.singletonList(session), rooms(60),
                Collections.<AutoSchedulingSolver.Fixed>emptyList(), Collections.<AutoSchedulingSolver.Preference>emptyList(),
                slots(1, 1));
        List<String> errors = solver.validatePlan(problem, Collections.singletonList(
                new AutoScheduleEntryDto(1, "A", "A", Collections.singletonList(10L), "教师",
                        Collections.singletonList("G1"), 0, 0, 0, 0, "")));
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("星期"));
    }

    private AutoSchedulePreviewDto solve(List<AutoSchedulingSolver.Session> sessions,
                                         List<AutoSchedulingSolver.Room> rooms,
                                         List<AutoSchedulingSolver.TimeSlot> slots) {
        return solver.solve(problem(sessions, rooms,
                Collections.<AutoSchedulingSolver.Fixed>emptyList(),
                Collections.<AutoSchedulingSolver.Preference>emptyList(), slots), 1000);
    }

    private static AutoSchedulingSolver.Problem problem(List<AutoSchedulingSolver.Session> sessions,
            List<AutoSchedulingSolver.Room> rooms, List<AutoSchedulingSolver.Fixed> fixed,
            List<AutoSchedulingSolver.Preference> preferences,
            List<AutoSchedulingSolver.TimeSlot> slots) {
        return new AutoSchedulingSolver.Problem(sessions, rooms, fixed, preferences, slots);
    }

    private static AutoSchedulingSolver.Session session(long id, String code, List<Long> teachers,
            String group, int capacity) { return session(id, code, teachers, group, capacity, null); }

    private static AutoSchedulingSolver.Session session(long id, String code, List<Long> teachers,
            String group, int capacity, String type) {
        return new AutoSchedulingSolver.Session(code, id, code, code, teachers, "教师", 
                Collections.singletonList(group), capacity, type);
    }

    private static List<AutoSchedulingSolver.Room> rooms(int... capacities) {
        List<AutoSchedulingSolver.Room> result = new ArrayList<AutoSchedulingSolver.Room>();
        for (int i = 0; i < capacities.length; i++) result.add(new AutoSchedulingSolver.Room(i + 1,
                "教室" + (i + 1), "TEACHING", capacities[i]));
        return result;
    }

    private static List<AutoSchedulingSolver.TimeSlot> slots(int day, int... periods) {
        List<AutoSchedulingSolver.TimeSlot> result = new ArrayList<AutoSchedulingSolver.TimeSlot>();
        for (int period : periods) result.add(new AutoSchedulingSolver.TimeSlot(day, period, period + 1));
        return result;
    }

    private static boolean overlaps(AutoScheduleEntryDto left, AutoScheduleEntryDto right) {
        return left.getWeekday() == right.getWeekday()
                && left.getStartPeriod() <= right.getEndPeriod()
                && right.getStartPeriod() <= left.getEndPeriod();
    }
}
