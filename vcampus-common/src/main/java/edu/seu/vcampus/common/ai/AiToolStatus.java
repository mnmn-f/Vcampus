package edu.seu.vcampus.common.ai;

import java.io.Serializable;

public final class AiToolStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String description;
    private final String parameterGuide;
    private final boolean writeOperation;
    private final boolean available;
    private final long calls;
    private final long succeeded;
    private final long failed;
    private final long averageDurationMillis;
    private final long lastCalledAt;
    private final String lastError;
    public AiToolStatus(String name, String description, String parameterGuide,
            boolean writeOperation, boolean available) {
        this(name, description, parameterGuide, writeOperation, available, 0, 0, 0, 0, 0, null);
    }
    public AiToolStatus(String name, String description, String parameterGuide,
            boolean writeOperation, boolean available, long calls, long succeeded, long failed,
            long averageDurationMillis, long lastCalledAt, String lastError) {
        this.name = name; this.description = description; this.parameterGuide = parameterGuide;
        this.writeOperation = writeOperation; this.available = available; this.calls = calls;
        this.succeeded = succeeded; this.failed = failed;
        this.averageDurationMillis = averageDurationMillis; this.lastCalledAt = lastCalledAt;
        this.lastError = lastError;
    }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getParameterGuide() { return parameterGuide; }
    public boolean isWriteOperation() { return writeOperation; }
    public boolean isAvailable() { return available; }
    public long getCalls() { return calls; }
    public long getSucceeded() { return succeeded; }
    public long getFailed() { return failed; }
    public long getAverageDurationMillis() { return averageDurationMillis; }
    public long getLastCalledAt() { return lastCalledAt; }
    public String getLastError() { return lastError; }
}
