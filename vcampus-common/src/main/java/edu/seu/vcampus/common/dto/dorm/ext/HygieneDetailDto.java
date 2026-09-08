package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** 一次卫生检查的完整视图：总分、等级、整改结论与五项明细。 */
public final class HygieneDetailDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String LEVEL_EXCELLENT = "EXCELLENT";
    public static final String LEVEL_GOOD = "GOOD";
    public static final String LEVEL_PASS = "PASS";
    public static final String LEVEL_POOR = "POOR";

    private final long inspectionId;
    private final long roomId;
    private final String buildingCode;
    private final String roomNo;
    private final long inspectorId;
    private final LocalDateTime inspectedAt;
    private final BigDecimal totalScore;
    private final String scoreLevel;
    private final boolean needRectify;
    private final LocalDate recheckDate;
    private final String issueDescription;
    private final List<HygieneItemScoreDto> items;

    public HygieneDetailDto(long inspectionId, long roomId, String buildingCode, String roomNo,
                            long inspectorId, LocalDateTime inspectedAt, BigDecimal totalScore,
                            String scoreLevel, boolean needRectify, LocalDate recheckDate,
                            String issueDescription, List<HygieneItemScoreDto> items) {
        this.inspectionId = inspectionId;
        this.roomId = roomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.inspectorId = inspectorId;
        this.inspectedAt = inspectedAt;
        this.totalScore = totalScore;
        this.scoreLevel = scoreLevel;
        this.needRectify = needRectify;
        this.recheckDate = recheckDate;
        this.issueDescription = issueDescription;
        this.items = items == null
                ? Collections.<HygieneItemScoreDto>emptyList()
                : Collections.unmodifiableList(new ArrayList<HygieneItemScoreDto>(items));
    }

    public long getInspectionId() { return inspectionId; }
    public long getRoomId() { return roomId; }
    public String getBuildingCode() { return buildingCode; }
    public String getRoomNo() { return roomNo; }
    public long getInspectorId() { return inspectorId; }
    public LocalDateTime getInspectedAt() { return inspectedAt; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getScoreLevel() { return scoreLevel; }
    /** 总分低于整改线时为真，服务端会同时建一条复查任务。 */
    public boolean isNeedRectify() { return needRectify; }
    /** 需要整改时的复查计划日期，否则为空。 */
    public LocalDate getRecheckDate() { return recheckDate; }
    public String getIssueDescription() { return issueDescription; }
    public List<HygieneItemScoreDto> getItems() { return items; }

    public static String levelName(String level) {
        if (LEVEL_EXCELLENT.equals(level)) return "优秀";
        if (LEVEL_GOOD.equals(level)) return "良好";
        if (LEVEL_PASS.equals(level)) return "合格";
        if (LEVEL_POOR.equals(level)) return "差";
        return level;
    }
}
