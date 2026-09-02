package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 将知识片段停用的请求。 */
public final class AiKnowledgeDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long chunkId;

    public AiKnowledgeDeleteRequest(long chunkId) { this.chunkId = chunkId; }
    public long getChunkId() { return chunkId; }
}
