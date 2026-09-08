package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** 反馈的管理处理状态和可选关联知识。 */
public final class AiFeedbackTriageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long feedbackId;
    private final String processStatus;
    private final Long relatedChunkId;
    public AiFeedbackTriageRequest(long feedbackId, String processStatus, Long relatedChunkId) {
        this.feedbackId = feedbackId; this.processStatus = processStatus;
        this.relatedChunkId = relatedChunkId;
    }
    public long getFeedbackId() { return feedbackId; }
    public String getProcessStatus() { return processStatus; }
    public Long getRelatedChunkId() { return relatedChunkId; }
}
