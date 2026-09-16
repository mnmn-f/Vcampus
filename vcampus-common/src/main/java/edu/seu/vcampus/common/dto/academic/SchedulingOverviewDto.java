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
    private final List<ClassroomDto> classrooms;
    private final List<String> semesterCodes;

    public SchedulingOverviewDto(List<SchedulingTeacherDto> teachers,
                                 List<TeacherTimePreferenceDto> preferences) {
        this(teachers, preferences, null, null);
    }

    public SchedulingOverviewDto(List<SchedulingTeacherDto> teachers,
                                 List<TeacherTimePreferenceDto> preferences,
                                 List<ClassroomDto> classrooms,
                                 List<String> semesterCodes) {
        this.teachers = immutable(teachers);
        this.preferences = immutable(preferences);
        this.classrooms = immutable(classrooms);
        this.semesterCodes = immutable(semesterCodes);
    }

    public List<SchedulingTeacherDto> getTeachers() { return teachers; }
    public List<TeacherTimePreferenceDto> getPreferences() { return preferences; }
    public List<ClassroomDto> getClassrooms() { return classrooms; }
    public List<String> getSemesterCodes() { return semesterCodes; }

    private static <T> List<T> immutable(List<T> values) {
        return values == null || values.isEmpty() ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
