package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 自动排课预览中的一条候选课次。 */
public final class AutoScheduleEntryDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long courseId;
    private final String courseCode;
    private final String courseName;
    private final List<Long> teacherUserIds;
    private final String teacherNames;
    private final List<String> studentGroups;
    private final int weekday;
    private final int startPeriod;
    private final int endPeriod;
    private final long classroomId;
    private final String classroomName;

    public AutoScheduleEntryDto(long courseId, String courseCode, String courseName,
            List<Long> teacherUserIds, String teacherNames, List<String> studentGroups,
            int weekday, int startPeriod, int endPeriod, long classroomId,
            String classroomName) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.teacherUserIds = immutable(teacherUserIds);
        this.teacherNames = teacherNames;
        this.studentGroups = immutable(studentGroups);
        this.weekday = weekday;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.classroomId = classroomId;
        this.classroomName = classroomName;
    }

    public long getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public List<Long> getTeacherUserIds() { return teacherUserIds; }
    public String getTeacherNames() { return teacherNames; }
    public List<String> getStudentGroups() { return studentGroups; }
    public int getWeekday() { return weekday; }
    public int getStartPeriod() { return startPeriod; }
    public int getEndPeriod() { return endPeriod; }
    public long getClassroomId() { return classroomId; }
    public String getClassroomName() { return classroomName; }

    private static <T> List<T> immutable(List<T> values) {
        return values == null || values.isEmpty() ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
