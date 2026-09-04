package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一门课程未退课（ENROLLED/COMPLETED）学生的只读花名册。 */
public final class CourseRosterDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long courseId;
    private final List<CourseRosterEntryDto> entries;

    public CourseRosterDto(long courseId, List<CourseRosterEntryDto> entries) {
        this.courseId = courseId;
        this.entries = entries == null || entries.isEmpty()
                ? Collections.<CourseRosterEntryDto>emptyList()
                : Collections.unmodifiableList(new ArrayList<CourseRosterEntryDto>(entries));
    }

    public long getCourseId() { return courseId; }
    public List<CourseRosterEntryDto> getEntries() { return entries; }
    public List<CourseRosterEntryDto> getItems() { return entries; }
}
