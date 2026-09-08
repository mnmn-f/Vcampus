package edu.seu.vcampus.server.ai.repository;

/** 单个校园工具的聚合运行指标。 */
public final class AiToolMetric {
    private final long calls, succeeded, failed, averageDurationMillis, lastCalledAt;
    private final String lastError;
    public AiToolMetric(long calls, long succeeded, long failed, long averageDurationMillis,
                        long lastCalledAt, String lastError) {
        this.calls = calls; this.succeeded = succeeded; this.failed = failed;
        this.averageDurationMillis = averageDurationMillis;
        this.lastCalledAt = lastCalledAt; this.lastError = lastError;
    }
    public long getCalls() { return calls; }
    public long getSucceeded() { return succeeded; }
    public long getFailed() { return failed; }
    public long getAverageDurationMillis() { return averageDurationMillis; }
    public long getLastCalledAt() { return lastCalledAt; }
    public String getLastError() { return lastError; }
}
