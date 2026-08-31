package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import java.math.BigDecimal;

/** 卫生检查或整改登记请求。id 为 0 表示新建。 */
public final class HygieneInspectionRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long roomId;
    private final BigDecimal score;
    private final String result;
    private final String issueDescription;
    private final String status;
    private final String rectificationNote;

    public HygieneInspectionRequest(long id, long roomId, BigDecimal score,
                                    String result, String issueDescription,
                                    String status, String rectificationNote) {
        this.id = id;
        this.roomId = roomId;
        this.score = score;
        this.result = result;
        this.issueDescription = issueDescription;
        this.status = status;
        this.rectificationNote = rectificationNote;
    }

    public HygieneInspectionRequest(long roomId, BigDecimal score, String result,
                                    String issueDescription) {
        this(0L, roomId, score, result, issueDescription, null, null);
    }

    public long getId() { return id; }
    public long getInspectionId() { return id; }
    public long getRoomId() { return roomId; }
    public BigDecimal getScore() { return score; }
    public String getResult() { return result; }
    public String getIssueDescription() { return issueDescription; }
    public String getStatus() { return status; }
    public String getRectificationNote() { return rectificationNote; }
}
