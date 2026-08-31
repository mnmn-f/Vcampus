package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.math.BigDecimal;

/** 本人校园账户充值请求；真实支付渠道可在此边界后接入。 */
public final class AccountRechargeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final BigDecimal amount;
    private final String idempotencyKey;
    private final String remark;

    public AccountRechargeRequest(BigDecimal amount, String idempotencyKey, String remark) {
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.remark = remark;
    }

    public AccountRechargeRequest(BigDecimal amount, String idempotencyKey) {
        this(amount, idempotencyKey, null);
    }

    public BigDecimal getAmount() { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRemark() { return remark; }
}
