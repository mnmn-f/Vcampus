package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;

/** 校园账户余额摘要。 */
public final class AccountDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final long userId;
    private final BigDecimal balance;
    private final String status;

    public AccountDto(long id, long userId, BigDecimal balance, String status) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.status = status;
    }

    public long getId() { return id; }
    public long getAccountId() { return id; }
    public long getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
}
