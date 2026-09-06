package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 自动排课搜索请求，搜索时间受服务端上限保护。 */
public final class AutoScheduleRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int timeLimitMillis;

    public AutoScheduleRequest(int timeLimitMillis) {
        this.timeLimitMillis = timeLimitMillis;
    }

    public int getTimeLimitMillis() { return timeLimitMillis; }
    public static AutoScheduleRequest defaults() { return new AutoScheduleRequest(8000); }
}
