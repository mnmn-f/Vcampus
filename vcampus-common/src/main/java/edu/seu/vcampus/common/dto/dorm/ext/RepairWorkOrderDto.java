package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/**
 * 维修员视角的报修工单。
 *
 * <p>和学生看到的 {@link RepairEntryPermitDto} 是同一张工单的两个视角，字段刻意不
 * 复用：学生关心「我那单批没批、师傅什么时候来」，维修员关心「在哪间房、什么活、
 * 急不急、能不能进门、进不去找谁」。硬塞进一个对象只会让两边都带着一半用不上的
 * 字段，而且哪些字段该给谁看也说不清楚。</p>
 *
 * <p>{@link #getContactPhone()} 只在工单已经派给本人时才会有值。未接的单在队列里
 * 只显示房间和活计——还没接手就先拿到学生电话，没有正当理由。</p>
 */
public final class RepairWorkOrderDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SUBMITTED = "SUBMITTED";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    /** 维修员报完工、等宿管审核。审核通过才算 COMPLETED。 */
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private final long orderId;
    private final long roomId;
    private final String buildingName;
    private final String roomNo;
    private final String category;
    private final String description;
    private final String priority;
    private final String status;
    private final Long reporterId;
    private final String reporterName;
    private final LocalDateTime submittedAt;
    private final LocalDateTime acceptedAt;
    private final LocalDateTime completedAt;
    private final Long handlerId;
    private final boolean entryAllowed;
    private final String entryNote;
    private final String contactPhone;
    private final Integer evaluationScore;

    public RepairWorkOrderDto(long orderId, long roomId, String buildingName, String roomNo,
                              String category, String description, String priority, String status,
                              Long reporterId, String reporterName, LocalDateTime submittedAt,
                              LocalDateTime acceptedAt, LocalDateTime completedAt, Long handlerId,
                              boolean entryAllowed, String entryNote, String contactPhone,
                              Integer evaluationScore) {
        this.orderId = orderId;
        this.roomId = roomId;
        this.buildingName = buildingName;
        this.roomNo = roomNo;
        this.category = category;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.submittedAt = submittedAt;
        this.acceptedAt = acceptedAt;
        this.completedAt = completedAt;
        this.handlerId = handlerId;
        this.entryAllowed = entryAllowed;
        this.entryNote = entryNote;
        this.contactPhone = contactPhone;
        this.evaluationScore = evaluationScore;
    }

    public long getOrderId() { return orderId; }
    public long getRoomId() { return roomId; }
    public String getBuildingName() { return buildingName; }
    public String getRoomNo() { return roomNo; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public Long getReporterId() { return reporterId; }
    public String getReporterName() { return reporterName; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Long getHandlerId() { return handlerId; }
    public boolean isEntryAllowed() { return entryAllowed; }
    public String getEntryNote() { return entryNote; }
    public String getContactPhone() { return contactPhone; }
    public Integer getEvaluationScore() { return evaluationScore; }

    /** 房间的可读位置，界面里到处都要拼这一串。 */
    public String location() {
        String building = buildingName == null ? "" : buildingName;
        String room = roomNo == null ? "" : roomNo;
        return building.isEmpty() ? room : building + " " + room;
    }

    /**
     * 学生不在时能不能自行进门。
     *
     * <p>没授权不等于不能修，只是得先联系人——所以这里只回答能不能进，联系方式由
     * {@link #getContactPhone()} 单独给。</p>
     */
    public boolean canEnterUnattended() {
        return entryAllowed;
    }
}
