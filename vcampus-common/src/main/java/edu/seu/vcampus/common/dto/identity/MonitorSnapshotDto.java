package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 最小系统监控快照，不暴露连接凭据或 SQL 细节。 */
public final class MonitorSnapshotDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean databaseHealthy;
    private final int activeSessionCount;
    private final long userCount;
    private final LocalDateTime checkedAt;
    private final String message;

    public MonitorSnapshotDto(boolean databaseHealthy, int activeSessionCount, long userCount,
                              LocalDateTime checkedAt, String message) {
        this.databaseHealthy = databaseHealthy;
        this.activeSessionCount = activeSessionCount;
        this.userCount = userCount;
        this.checkedAt = checkedAt;
        this.message = message;
    }

    public boolean isDatabaseHealthy() { return databaseHealthy; }
    public boolean getDatabaseHealthy() { return databaseHealthy; }
    public int getActiveSessionCount() { return activeSessionCount; }
    public long getUserCount() { return userCount; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public String getMessage() { return message; }
}
