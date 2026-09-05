package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/**
 * 学生「我的住宿」首屏摘要。
 *
 * <p>首屏要同时回答四件事：这间房住了几个人、这个月水电欠多少、上次卫生检查多少
 * 分、有没有待办。它们分属住宿、账单、卫生三块数据，如果由界面分三次请求拼装，
 * 一是三次往返，二是三份数据可能来自不同时刻，页面上会出现「卫生分已经复查通过、
 * 提示条却还在喊整改」这种自相矛盾的画面。所以在服务端一次算完再发回来。</p>
 *
 * <p>刻意只带首屏要显示的字段，不带明细：明细各自有自己的查询命令，塞进来只会让
 * 这个对象随着页面改版不断膨胀。</p>
 */
public final class DormHomeSummaryDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 卫生检查判定：不合格。取值与 hygiene_inspections.result 一致。 */
    public static final String HYGIENE_FAIL = "FAIL";
    /** 卫生整改状态：需整改。取值与 hygiene_inspections.status 一致。 */
    public static final String RECTIFICATION_REQUIRED = "RECTIFICATION_REQUIRED";

    private final boolean resident;
    private final long roomId;
    private final String buildingName;
    private final String roomNo;
    private final String bedNo;
    private final int capacity;
    private final int occupiedBeds;
    private final BigDecimal unpaidAmount;
    private final int unpaidBills;
    private final BigDecimal hygieneScore;
    private final String hygieneResult;
    private final String hygieneStatus;
    private final String hygieneIssue;
    private final LocalDateTime hygieneInspectedAt;
    private final int pendingRequests;

    public DormHomeSummaryDto(boolean resident, long roomId, String buildingName, String roomNo,
                              String bedNo, int capacity, int occupiedBeds,
                              BigDecimal unpaidAmount, int unpaidBills,
                              BigDecimal hygieneScore, String hygieneResult, String hygieneStatus,
                              String hygieneIssue, LocalDateTime hygieneInspectedAt,
                              int pendingRequests) {
        this.resident = resident;
        this.roomId = roomId;
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.bedNo = bedNo;
        this.capacity = capacity;
        this.occupiedBeds = occupiedBeds;
        this.unpaidAmount = unpaidAmount;
        this.unpaidBills = unpaidBills;
        this.hygieneScore = hygieneScore;
        this.hygieneResult = hygieneResult;
        this.hygieneStatus = hygieneStatus;
        this.hygieneIssue = hygieneIssue;
        this.hygieneInspectedAt = hygieneInspectedAt;
        this.pendingRequests = pendingRequests;
    }

    /** 没有在住记录时的空摘要：界面照常渲染，只是每一格都显示占位。 */
    public static DormHomeSummaryDto empty() {
        return new DormHomeSummaryDto(false, 0L, null, null, null, 0, 0,
                BigDecimal.ZERO, 0, null, null, null, null, null, 0);
    }

    public boolean isResident() { return resident; }
    public long getRoomId() { return roomId; }
    public String getBuildingName() { return buildingName; }
    public String getRoomNo() { return roomNo; }
    public String getBedNo() { return bedNo; }
    public int getCapacity() { return capacity; }
    public int getOccupiedBeds() { return occupiedBeds; }
    public BigDecimal getUnpaidAmount() { return unpaidAmount; }
    public int getUnpaidBills() { return unpaidBills; }
    public BigDecimal getHygieneScore() { return hygieneScore; }
    public String getHygieneResult() { return hygieneResult; }
    public String getHygieneStatus() { return hygieneStatus; }
    public String getHygieneIssue() { return hygieneIssue; }
    public LocalDateTime getHygieneInspectedAt() { return hygieneInspectedAt; }
    public int getPendingRequests() { return pendingRequests; }

    /**
     * 是否还欠一次整改——界面据此决定要不要弹红色提示条。
     *
     * <p>判据是整改状态而不是分数：分数低但宿管标了「已整改」的房间不该再被催，
     * 而 {@code RECTIFICATION_REQUIRED} 正是宿管明确要求返工的那一档。</p>
     */
    public boolean needsRectification() {
        return RECTIFICATION_REQUIRED.equals(hygieneStatus);
    }

    /** 待办条数：未缴账单 + 待审批申请 + 待整改卫生，界面只显示这一个数。 */
    public int todoCount() {
        return unpaidBills + pendingRequests + (needsRectification() ? 1 : 0);
    }
}
