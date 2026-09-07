package edu.seu.vcampus.server.academic.scheduling;

import edu.seu.vcampus.common.dto.academic.AutoScheduleEntryDto;
import edu.seu.vcampus.common.dto.academic.SchedulingOverviewDto;
import edu.seu.vcampus.common.dto.academic.TeacherTimePreferenceDto;
import java.sql.Connection;
import java.util.List;

/** Persistence boundary dedicated to automatic scheduling. */
public interface SchedulingRepository {
    AutoSchedulingSolver.Problem loadProblem(Connection connection) throws Exception;
    SchedulingOverviewDto overview(Connection connection) throws Exception;
    TeacherTimePreferenceDto savePreference(Connection connection,
                                             TeacherTimePreferenceDto value) throws Exception;
    boolean deletePreference(Connection connection, long id) throws Exception;
    void lockSchedules(Connection connection) throws Exception;
    int savePlan(Connection connection, List<AutoScheduleEntryDto> entries) throws Exception;
}
