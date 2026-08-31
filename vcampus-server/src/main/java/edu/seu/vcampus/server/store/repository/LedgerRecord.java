package edu.seu.vcampus.server.store.repository;

import java.math.BigDecimal;

/** 幂等键查询所需的最小流水信息，不把数据库行对象暴露给服务层。 */
public final class LedgerRecord {
    private final long accountId;
    private final BigDecimal amount;
    private final Long referenceId;
    private final String referenceType;
    private final String transactionType;

    public LedgerRecord(long accountId, BigDecimal amount, String referenceType,
                        Long referenceId, String transactionType) {
        this.accountId = accountId;
        this.amount = amount;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.transactionType = transactionType;
    }

    public long getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public Long getReferenceId() { return referenceId; }
    public String getReferenceType() { return referenceType; }
    public String getTransactionType() { return transactionType; }
}
