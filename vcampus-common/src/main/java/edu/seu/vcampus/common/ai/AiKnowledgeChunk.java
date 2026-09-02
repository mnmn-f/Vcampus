package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 校园知识库中的一个可检索片段。 */
public final class AiKnowledgeChunk implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long chunkId;
    private final String sourceType;
    private final String title;
    private final String content;
    private final String status;
    private final long updatedAt;

    public AiKnowledgeChunk(long chunkId, String sourceType, String title,
                            String content, String status, long updatedAt) {
        this.chunkId = chunkId;
        this.sourceType = sourceType;
        this.title = title;
        this.content = content;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public long getChunkId() { return chunkId; }
    public String getSourceType() { return sourceType; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public long getUpdatedAt() { return updatedAt; }
}
