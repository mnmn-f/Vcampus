package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 图书检索条件；空字符串表示不限制该条件。 */
public final class BookSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String keyword;
    private final String category;
    private final String status;
    private final int page;
    private final int pageSize;

    public BookSearchRequest(String keyword, String category, String status,
                             int page, int pageSize) {
        this.keyword = keyword;
        this.category = category;
        this.status = status;
        this.page = page;
        this.pageSize = pageSize;
    }

    public BookSearchRequest(String keyword, int page, int pageSize) {
        this(keyword, null, null, page, pageSize);
    }

    public BookSearchRequest() {
        this(null, null, null, 1, 20);
    }

    public String getKeyword() { return keyword; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
