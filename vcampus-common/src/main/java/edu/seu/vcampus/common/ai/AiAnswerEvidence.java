package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 回答所依据的知识片段摘要；不包含提示词或敏感业务数据。 */
public final class AiAnswerEvidence implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long chunkId;
    private final String title;
    private final String sourceType;
    private final String excerpt;
    private final long updatedAt;

    public AiAnswerEvidence(long chunkId, String title, String sourceType,
            String excerpt, long updatedAt) {
        this.chunkId = chunkId; this.title = title; this.sourceType = sourceType;
        this.excerpt = excerpt; this.updatedAt = updatedAt;
    }

    public long getChunkId() { return chunkId; }
    public String getTitle() { return title; }
    public String getSourceType() { return sourceType; }
    public String getExcerpt() { return excerpt; }
    public long getUpdatedAt() { return updatedAt; }
}
