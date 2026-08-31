package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 删除课程时段的最小请求。 */
public final class ScheduleIdRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long scheduleId;

    public ScheduleIdRequest(long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public long getScheduleId() {
        return scheduleId;
    }

    public long getId() {
        return scheduleId;
    }
}
