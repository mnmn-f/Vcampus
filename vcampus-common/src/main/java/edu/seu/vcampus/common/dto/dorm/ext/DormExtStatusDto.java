package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.threeten.bp.LocalDateTime;

/** 宿舍扩展模块运行状态；对应设计文档的服务端服务监控界面。 */
public final class DormExtStatusDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String moduleVersion;
    private final boolean schedulerRunning;
    private final List<String> scheduledTasks;
    private final LocalDateTime serverTime;

    public DormExtStatusDto(String moduleVersion, boolean schedulerRunning,
                            List<String> scheduledTasks, LocalDateTime serverTime) {
        this.moduleVersion = moduleVersion;
        this.schedulerRunning = schedulerRunning;
        this.scheduledTasks = scheduledTasks == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(scheduledTasks));
        this.serverTime = serverTime;
    }

    public String getModuleVersion() { return moduleVersion; }
    /** 定时任务调度器是否已启动；P0 阶段固定为 false。 */
    public boolean isSchedulerRunning() { return schedulerRunning; }
    public List<String> getScheduledTasks() { return scheduledTasks; }
    public LocalDateTime getServerTime() { return serverTime; }
}
