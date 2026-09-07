package edu.seu.vcampus.common.ai;

import java.io.Serializable;

public final class AiKnowledgeVersion implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long versionId;
    private final long chunkId;
    private final int versionNo;
    private final String sourceType;
    private final String title;
    private final String content;
    private final String status;
    private final long createdAt;
    public AiKnowledgeVersion(long versionId, long chunkId, int versionNo, String sourceType,
            String title, String content, String status, long createdAt) {
        this.versionId = versionId; this.chunkId = chunkId; this.versionNo = versionNo;
        this.sourceType = sourceType; this.title = title; this.content = content;
        this.status = status; this.createdAt = createdAt;
    }
    public long getVersionId() { return versionId; }
    public long getChunkId() { return chunkId; }
    public int getVersionNo() { return versionNo; }
    public String getSourceType() { return sourceType; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    @Override public String toString() { return "版本 " + versionNo + " · " + new java.util.Date(createdAt); }
}
