package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 教务老师新增或修改课程时段的请求。 */
public final class ScheduleSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long scheduleId;
    private final long courseId;
    private final int weekday;
    private final int startPeriod;
    private final int endPeriod;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Long classroomId;

    public ScheduleSaveRequest(Long scheduleId, long courseId, int weekday,
                               int startPeriod, int endPeriod, LocalDate startDate,
                               LocalDate endDate, Long classroomId) {
        this.scheduleId = scheduleId;
        this.courseId = courseId;
        this.weekday = weekday;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.startDate = startDate;
        this.endDate = endDate;
        this.classroomId = classroomId;
    }

    public static ScheduleSaveRequest create(long courseId, int weekday,
                                             int startPeriod, int endPeriod,
                                             LocalDate startDate, LocalDate endDate,
                                             Long classroomId) {
        return new ScheduleSaveRequest(null, courseId, weekday, startPeriod,
                endPeriod, startDate, endDate, classroomId);
    }

    public static ScheduleSaveRequest update(long id, long courseId, int weekday,
                                             int startPeriod, int endPeriod,
                                             LocalDate startDate, LocalDate endDate,
                                             Long classroomId) {
        if (id <= 0) {
            throw new IllegalArgumentException("schedule id must be positive");
        }
        return new ScheduleSaveRequest(Long.valueOf(id), courseId, weekday,
                startPeriod, endPeriod, startDate, endDate, classroomId);
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public long getCourseId() {
        return courseId;
    }

    public int getWeekday() {
        return weekday;
    }

    public int getStartPeriod() {
        return startPeriod;
    }

    public int getEndPeriod() {
        return endPeriod;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public boolean isUpdate() {
        return scheduleId != null;
    }
}
