package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 管理端反馈摘要；不包含学生姓名、学号或完整会话。 */
public final class AiFeedbackEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String rating;
    private final String category;
    private final String comment;
    private final String questionPreview;
    private final long createdAt;
    private final String processStatus;
    private final Long relatedChunkId;
    private final String relatedChunkTitle;
    public AiFeedbackEntry(long id, String rating, String category, String comment,
            String questionPreview, long createdAt) {
        this(id, rating, category, comment, questionPreview, createdAt, "PENDING", null, null);
    }
    public AiFeedbackEntry(long id, String rating, String category, String comment,
            String questionPreview, long createdAt, String processStatus,
            Long relatedChunkId, String relatedChunkTitle) {
        this.id = id; this.rating = rating; this.category = category;
        this.comment = comment; this.questionPreview = questionPreview; this.createdAt = createdAt;
        this.processStatus = processStatus; this.relatedChunkId = relatedChunkId;
        this.relatedChunkTitle = relatedChunkTitle;
    }
    public long getId() { return id; }
    public String getRating() { return rating; }
    public String getCategory() { return category; }
    public String getComment() { return comment; }
    public String getQuestionPreview() { return questionPreview; }
    public long getCreatedAt() { return createdAt; }
    public String getProcessStatus() { return processStatus == null ? "PENDING" : processStatus; }
    public Long getRelatedChunkId() { return relatedChunkId; }
    public String getRelatedChunkTitle() { return relatedChunkTitle; }
}
