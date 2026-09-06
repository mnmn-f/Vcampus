package edu.seu.vcampus.common.ai;

import java.io.Serializable;

public final class AiKnowledgeVersionRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long chunkId;
    private final Long versionId;
    public AiKnowledgeVersionRequest(long chunkId, Long versionId) {
        this.chunkId = chunkId; this.versionId = versionId;
    }
    public long getChunkId() { return chunkId; }
    public Long getVersionId() { return versionId; }
}
