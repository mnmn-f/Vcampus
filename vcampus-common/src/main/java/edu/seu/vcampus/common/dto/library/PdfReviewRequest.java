package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfReviewRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long resourceId;
    private final String decision;
    private final String reason;

    public PdfReviewRequest(long resourceId, String decision, String reason) {
        this.resourceId = resourceId;
        this.decision = decision;
        this.reason = reason;
    }
    public long getResourceId() { return resourceId; }
    public String getDecision() { return decision; }
    public String getReason() { return reason; }
}
