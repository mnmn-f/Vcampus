package edu.seu.vcampus.common.dto.store;

import java.util.List;

/** 账户流水分页结果。 */
public final class AccountLedgerPage extends StorePage<AccountTransactionDto> {
    private static final long serialVersionUID = 1L;

    public AccountLedgerPage(List<AccountTransactionDto> items, int page,
                             int pageSize, long total) {
        super(items, page, pageSize, total);
    }

    public AccountLedgerPage(int page, int pageSize, long total,
                             List<AccountTransactionDto> items) {
        this(items, page, pageSize, total);
    }

    public List<AccountTransactionDto> getTransactions() { return getItems(); }
}
