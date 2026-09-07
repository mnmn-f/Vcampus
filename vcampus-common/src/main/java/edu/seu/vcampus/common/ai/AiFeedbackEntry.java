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
    public AiFeedbackEntry(long id, String rating, String category, String comment,
            String questionPreview, long createdAt) {
        this.id = id; this.rating = rating; this.category = category;
        this.comment = comment; this.questionPreview = questionPreview; this.createdAt = createdAt;
    }
    public long getId() { return id; }
    public String getRating() { return rating; }
    public String getCategory() { return category; }
    public String getComment() { return comment; }
    public String getQuestionPreview() { return questionPreview; }
    public long getCreatedAt() { return createdAt; }
}
