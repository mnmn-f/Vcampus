package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 本人账户流水分页条件。 */
public final class AccountLedgerQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final String transactionType;
    private final String keyword;
    private final int page;
    private final int pageSize;

    public AccountLedgerQuery() { this(null, null, 1, DEFAULT_PAGE_SIZE); }

    public AccountLedgerQuery(String transactionType, int page, int pageSize) {
        this(transactionType, null, page, pageSize);
    }

    public AccountLedgerQuery(String transactionType, String keyword, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.transactionType = text(transactionType);
        this.keyword = text(keyword);
        this.page = page;
        this.pageSize = pageSize;
    }

    public AccountLedgerQuery(int page, int pageSize) {
        this(null, page, pageSize);
    }

    public String getTransactionType() { return transactionType; }
    public String getKeyword() { return keyword; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
