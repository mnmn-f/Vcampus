package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 自动排课搜索请求，搜索时间受服务端上限保护。 */
public final class AutoScheduleRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int timeLimitMillis;
    private final String semesterCode;

    public AutoScheduleRequest(int timeLimitMillis) {
        this(timeLimitMillis, null);
    }

    public AutoScheduleRequest(int timeLimitMillis, String semesterCode) {
        this.timeLimitMillis = timeLimitMillis;
        this.semesterCode = semesterCode;
    }

    public int getTimeLimitMillis() { return timeLimitMillis; }
    public String getSemesterCode() { return semesterCode; }
    public static AutoScheduleRequest defaults() { return new AutoScheduleRequest(8000); }
}
