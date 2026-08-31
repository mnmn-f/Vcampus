package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 课程时段及其教室摘要。 */
public final class CourseScheduleDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long courseId;
    private final int weekday;
    private final int startPeriod;
    private final int endPeriod;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ClassroomDto classroom;

    public CourseScheduleDto(long id, long courseId, int weekday, int startPeriod,
                             int endPeriod, LocalDate startDate, LocalDate endDate,
                             ClassroomDto classroom) {
        this.id = id;
        this.courseId = courseId;
        this.weekday = weekday;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.startDate = startDate;
        this.endDate = endDate;
        this.classroom = classroom;
    }

    public long getId() { return id; }
    public long getScheduleId() { return id; }
    public long getCourseId() { return courseId; }
    public int getWeekday() { return weekday; }
    public int getStartPeriod() { return startPeriod; }
    public int getEndPeriod() { return endPeriod; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public ClassroomDto getClassroom() { return classroom; }
}
