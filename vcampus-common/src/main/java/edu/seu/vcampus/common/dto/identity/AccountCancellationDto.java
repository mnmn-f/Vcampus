package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 脱敏账号注销申请记录；不包含密码、会话令牌等凭据。 */
public final class AccountCancellationDto implements Serializable {
    private static final long serialVersionUID = 1L;
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

    public AccountCancellationDto(long id, long userId, String account, String displayName,
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
    public long getRequestId() { return id; }
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
}
