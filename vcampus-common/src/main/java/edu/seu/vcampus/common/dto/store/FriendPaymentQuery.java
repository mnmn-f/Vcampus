package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.Locale;

/** 好友代付分页查询。scope 为 MINE 或 INBOX。 */
public final class FriendPaymentQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final String scope;
    private final int page;
    private final int pageSize;
    public FriendPaymentQuery(String scope, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("代付分页参数不正确");
        }
        String value = scope == null ? "INBOX" : scope.trim().toUpperCase(Locale.ROOT);
        if (!"MINE".equals(value) && !"INBOX".equals(value)) {
            throw new IllegalArgumentException("代付查询范围不正确");
        }
        this.scope = value;
        this.page = page; this.pageSize = pageSize;
    }
    public FriendPaymentQuery(String scope) { this(scope, 1, 20); }
    public String getScope() { return scope; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }
}
