package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 自动排课配置所需的教师与偏好快照。 */
public final class SchedulingOverviewDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<SchedulingTeacherDto> teachers;
    private final List<TeacherTimePreferenceDto> preferences;

    public SchedulingOverviewDto(List<SchedulingTeacherDto> teachers,
                                 List<TeacherTimePreferenceDto> preferences) {
        this.teachers = immutable(teachers);
        this.preferences = immutable(preferences);
    }

    public List<SchedulingTeacherDto> getTeachers() { return teachers; }
    public List<TeacherTimePreferenceDto> getPreferences() { return preferences; }

    private static <T> List<T> immutable(List<T> values) {
        return values == null || values.isEmpty() ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
