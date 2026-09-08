package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/**
 * 外来人员来访登记视图。
 *
 * <p>证件号只以掩码形式出现在本类里：登记提交后，任何查询场景都不需要完整证件号，
 * 而查询结果会经过网络、进入表格、可能被截图。完整值只留在数据库中供线下核验，
 * 这与项目「日志、审计、DTO 不得记录明文口令」的既有约定是同一条思路。</p>
 */
public final class VisitorRegistrationDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final long id;
    private final long studentUserId;
    private final long roomId;
    private final String buildingCode;
    private final String roomNo;
    private final String visitorName;
    private final String visitorIdCardMasked;
    private final String visitorPhone;
    private final String visitReason;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final LocalDateTime submittedAt;
    private final String auditStatus;
    private final Long auditorId;
    private final LocalDateTime auditedAt;
    private final String auditRemark;

    public VisitorRegistrationDto(long id, long studentUserId, long roomId, String buildingCode,
                                  String roomNo, String visitorName, String visitorIdCardMasked,
                                  String visitorPhone, String visitReason, LocalDateTime startAt,
                                  LocalDateTime endAt, LocalDateTime submittedAt,
                                  String auditStatus, Long auditorId, LocalDateTime auditedAt,
                                  String auditRemark) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.roomId = roomId;
        this.buildingCode = buildingCode;
        this.roomNo = roomNo;
        this.visitorName = visitorName;
        this.visitorIdCardMasked = visitorIdCardMasked;
        this.visitorPhone = visitorPhone;
        this.visitReason = visitReason;
        this.startAt = startAt;
        this.endAt = endAt;
        this.submittedAt = submittedAt;
        this.auditStatus = auditStatus;
        this.auditorId = auditorId;
        this.auditedAt = auditedAt;
        this.auditRemark = auditRemark;
    }

    /** 把证件号处理成只保留头尾的掩码，例如 320***********12。 */
    public static String mask(String idCard) {
        if (idCard == null) return null;
        String value = idCard.trim();
        if (value.length() <= 5) return "****";
        StringBuilder masked = new StringBuilder(value.substring(0, 3));
        for (int i = 3; i < value.length() - 2; i++) masked.append('*');
        return masked.append(value.substring(value.length() - 2)).toString();
    }

    public long getId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public long getRoomId() { return roomId; }
    public String getBuildingCode() { return buildingCode; }
    public String getRoomNo() { return roomNo; }
    public String getVisitorName() { return visitorName; }
    /** 掩码后的证件号；系统不会通过任何查询接口回传完整值。 */
    public String getVisitorIdCardMasked() { return visitorIdCardMasked; }
    public String getVisitorPhone() { return visitorPhone; }
    public String getVisitReason() { return visitReason; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getAuditStatus() { return auditStatus; }
    public Long getAuditorId() { return auditorId; }
    public LocalDateTime getAuditedAt() { return auditedAt; }
    public String getAuditRemark() { return auditRemark; }

    public boolean isPending() { return STATUS_PENDING.equals(auditStatus); }
}
