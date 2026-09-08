package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfDownloadRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long userId;
    private final long resourceId;
    private final String title;
    private final String fileName;
    private final long fileSize;
    private final String sha256;
    private final long transferred;
    private final String status;
    private final long startedAt;
    private final long finishedAt;
    private final String failureReason;

    public PdfDownloadRecord(long id, long userId, long resourceId, String title, String fileName, long fileSize, String sha256, long transferred, String status, long startedAt, long finishedAt, String failureReason) {
        this.id = id;
        this.userId = userId;
        this.resourceId = resourceId;
        this.title = title;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        this.transferred = transferred;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.failureReason = failureReason;
    }
    public long getId() { return id; }
    public long getUserId() { return userId; }
    public long getResourceId() { return resourceId; }
    public String getTitle() { return title; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getSha256() { return sha256; }
    public long getTransferred() { return transferred; }
    public String getStatus() { return status; }
    public long getStartedAt() { return startedAt; }
    public long getFinishedAt() { return finishedAt; }
    public String getFailureReason() { return failureReason; }
}
