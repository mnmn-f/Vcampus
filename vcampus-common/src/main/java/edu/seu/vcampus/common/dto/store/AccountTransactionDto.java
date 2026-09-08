package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;
import org.threeten.bp.LocalDateTime;

/** 账户流水，金额为有符号值。 */
public final class AccountTransactionDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long accountId;
    private final String transactionType;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final String referenceType;
    private final Long referenceId;
    private final String idempotencyKey;
    private final Long operatorId;
    private final String remark;
    private final LocalDateTime createdAt;

    public AccountTransactionDto(long id, long accountId, String transactionType,
                                 BigDecimal amount, BigDecimal balanceBefore,
                                 BigDecimal balanceAfter, String referenceType,
                                 Long referenceId, String idempotencyKey, Long operatorId,
                                 String remark, LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.operatorId = operatorId;
        this.remark = remark;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getTransactionId() { return id; }
    public long getAccountId() { return accountId; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public BigDecimal getBalance() { return balanceAfter; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getOperatorId() { return operatorId; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
