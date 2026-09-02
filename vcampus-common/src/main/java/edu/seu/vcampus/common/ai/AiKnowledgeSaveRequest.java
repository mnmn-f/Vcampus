package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 新增或更新知识片段。chunkId 为空表示新增。 */
public final class AiKnowledgeSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long chunkId;
    private final String sourceType;
    private final String title;
    private final String content;
    private final String status;

    public AiKnowledgeSaveRequest(Long chunkId, String sourceType, String title,
                                  String content, String status) {
        this.chunkId = chunkId;
        this.sourceType = sourceType;
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public Long getChunkId() { return chunkId; }
    public String getSourceType() { return sourceType; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
}
