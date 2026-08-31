package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;

import org.threeten.bp.LocalDateTime;

/** 账号注销申请的内部记录；不保存任何密码或会话凭据。 */
public final class AccountCancellationRecord {
    private final long id;
    private final long userId;
    private final String account;
    private final String displayName;
    private final String reason;
    private final String status;
    private final Long reviewedBy;
    private final LocalDateTime reviewedAt;
    private final String reviewRemark;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AccountCancellationRecord(long id, long userId, String account, String displayName,
                                     String reason, String status, Long reviewedBy,
                                     LocalDateTime reviewedAt, String reviewRemark,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.account = account;
        this.displayName = displayName;
        this.reason = reason;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewRemark = reviewRemark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getAccount() { return account; }
    public String getDisplayName() { return displayName; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public Long getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewRemark() { return reviewRemark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public AccountCancellationDto toDto() {
        return new AccountCancellationDto(id, userId, account, displayName, reason, status,
                reviewedBy, reviewedAt, reviewRemark, createdAt, updatedAt);
    }
}
