package edu.seu.vcampus.common.validation;
import org.junit.Test;
import static org.junit.Assert.*;
public class StudentCohortYearsTest {
    @Test public void sameCohortAlwaysHasSameYears() {
        assertEquals(Integer.valueOf(2026), StudentCohortYears.enrollment("建筑学2026级2班"));
        assertEquals(Integer.valueOf(2031), StudentCohortYears.graduation("建筑学2026级2班"));
        assertEquals(Integer.valueOf(2029), StudentCohortYears.graduation("建筑学2024级1班"));
        assertEquals(Integer.valueOf(2028), StudentCohortYears.graduation("计算机科学与技术2024级1班"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsChangedGraduationYear() {
        StudentCohortYears.validate("建筑学2026级2班", 2026, 2032);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsChangedEnrollmentYear() {
        StudentCohortYears.validate("建筑学2026级2班", 2024, 2030);
    }
}
