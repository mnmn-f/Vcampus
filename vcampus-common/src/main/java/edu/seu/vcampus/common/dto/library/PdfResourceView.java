package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfResourceView implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String title;
    private final String description;
    private final String fileName;
    private final long fileSize;
    private final String sha256;
    private final long uploaderId;
    private final String uploaderName;
    private final String status;
    private final long uploadedAt;
    private final long reviewerId;
    private final String reviewerName;
    private final long reviewedAt;
    private final String rejectionReason;

    public PdfResourceView(long id, String title, String description, String fileName, long fileSize, String sha256, long uploaderId, String uploaderName, String status, long uploadedAt, long reviewerId, String reviewerName, long reviewedAt, String rejectionReason) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        this.uploaderId = uploaderId;
        this.uploaderName = uploaderName;
        this.status = status;
        this.uploadedAt = uploadedAt;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewedAt = reviewedAt;
        this.rejectionReason = rejectionReason;
    }
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getSha256() { return sha256; }
    public long getUploaderId() { return uploaderId; }
    public String getUploaderName() { return uploaderName; }
    public String getStatus() { return status; }
    public long getUploadedAt() { return uploadedAt; }
    public long getReviewerId() { return reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public long getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
}
