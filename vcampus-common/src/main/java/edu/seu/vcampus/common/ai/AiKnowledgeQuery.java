package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 管理端知识片段查询条件。 */
public final class AiKnowledgeQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String keyword;
    private final String status;
    private final int page;
    private final int pageSize;
    private final Long chunkId;

    public AiKnowledgeQuery(String keyword, String status, int page, int pageSize) {
        this(keyword, status, page, pageSize, null);
    }

    public AiKnowledgeQuery(String keyword, String status, int page, int pageSize, Long chunkId) {
        this.keyword = keyword;
        this.status = status;
        this.page = page;
        this.pageSize = pageSize;
        this.chunkId = chunkId;
    }

    public AiKnowledgeQuery() { this(null, null, 1, 20); }
    public String getKeyword() { return keyword; }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public Long getChunkId() { return chunkId; }
}
