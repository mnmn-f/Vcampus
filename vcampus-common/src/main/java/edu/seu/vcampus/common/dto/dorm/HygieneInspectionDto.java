package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 卫生检查视图。 */
public final class HygieneInspectionDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long roomId;
    private final long inspectorId;
    private final LocalDateTime inspectedAt;
    private final BigDecimal score;
    private final String result;
    private final String issueDescription;
    private final String status;
    private final LocalDateTime rectifiedAt;
    private final String rectificationNote;

    public HygieneInspectionDto(long id, long roomId, long inspectorId,
                                LocalDateTime inspectedAt, BigDecimal score,
                                String result, String issueDescription, String status,
                                LocalDateTime rectifiedAt, String rectificationNote) {
        this.id = id;
        this.roomId = roomId;
        this.inspectorId = inspectorId;
        this.inspectedAt = inspectedAt;
        this.score = score;
        this.result = result;
        this.issueDescription = issueDescription;
        this.status = status;
        this.rectifiedAt = rectifiedAt;
        this.rectificationNote = rectificationNote;
    }

    public long getId() { return id; }
    public long getInspectionId() { return id; }
    public long getRoomId() { return roomId; }
    public long getInspectorId() { return inspectorId; }
    public LocalDateTime getInspectedAt() { return inspectedAt; }
    public BigDecimal getScore() { return score; }
    public String getResult() { return result; }
    public String getIssueDescription() { return issueDescription; }
    public String getStatus() { return status; }
    public LocalDateTime getRectifiedAt() { return rectifiedAt; }
    public String getRectificationNote() { return rectificationNote; }
}
