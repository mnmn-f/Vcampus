package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;

/** 商品分页查询；学生默认只看到服务端限定的在售商品。 */
public final class ProductQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final String keyword;
    private final String category;
    private final String status;
    private final int page;
    private final int pageSize;

    public ProductQuery() { this(null, null, null, 1, DEFAULT_PAGE_SIZE); }

    public ProductQuery(String keyword, String category, String status,
                        int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.keyword = text(keyword);
        this.category = text(category);
        this.status = text(status);
        this.page = page;
        this.pageSize = pageSize;
    }

    public ProductQuery(int page, int pageSize, String keyword, String category,
                        String status) {
        this(keyword, category, status, page, pageSize);
    }

    public ProductQuery(String keyword, int page, int pageSize) {
        this(keyword, null, null, page, pageSize);
    }

    public String getKeyword() { return keyword; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
