package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 可持久化的教师时间偏好。 */
public final class TeacherTimePreferenceDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long teacherUserId;
    private final int weekday;
    private final int startPeriod;
    private final int endPeriod;
    private final String preferenceType;

    public TeacherTimePreferenceDto(long id, long teacherUserId, int weekday,
                                    int startPeriod, int endPeriod, String preferenceType) {
        this.id = id;
        this.teacherUserId = teacherUserId;
        this.weekday = weekday;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.preferenceType = preferenceType;
    }

    public long getId() { return id; }
    public long getTeacherUserId() { return teacherUserId; }
    public int getWeekday() { return weekday; }
    public int getStartPeriod() { return startPeriod; }
    public int getEndPeriod() { return endPeriod; }
    public String getPreferenceType() { return preferenceType; }
}
