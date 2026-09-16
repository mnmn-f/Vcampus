package edu.seu.vcampus.common.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared year policy for the application's named undergraduate cohorts. */
public final class StudentCohortYears {
    private static final Pattern COHORT = Pattern.compile("^(.+)(20[0-9]{2})级[0-9]+班$");
    private StudentCohortYears() { }

    public static Integer enrollment(String className) {
        Matcher match = COHORT.matcher(className == null ? "" : className.trim());
        return match.matches() ? Integer.valueOf(match.group(2)) : null;
    }

    public static Integer graduation(String className) {
        Matcher match = COHORT.matcher(className == null ? "" : className.trim());
        if (!match.matches()) return null;
        int year = Integer.parseInt(match.group(2));
        String major = match.group(1);
        // SEU architecture: five years; planning/landscape: four years for 2026 intake.
        boolean fiveYears = "建筑学".equals(major) || (year < 2026
                && ("城乡规划".equals(major) || "风景园林".equals(major)));
        return year + (fiveYears ? 5 : 4);
    }

    public static void validate(String className, Integer enrollment, Integer graduation) {
        Integer expected = enrollment(className);
        if (expected == null) return; // Preserve non-catalog historical class formats.
        if (!expected.equals(enrollment) || !graduation(className).equals(graduation)) {
            throw new IllegalArgumentException("入学和预计毕业年份须与所选班级一致");
        }
    }
}
