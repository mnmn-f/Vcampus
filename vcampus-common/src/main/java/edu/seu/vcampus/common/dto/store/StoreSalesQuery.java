package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

/** 商店销售统计筛选；日期范围按自然日闭区间解释。 */
public final class StoreSalesQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Long productId;
    private final String keyword;
    private final int page;
    private final int pageSize;

    public StoreSalesQuery() {
        this(null, null, (Long) null, null, 1, DEFAULT_PAGE_SIZE);
    }

    public StoreSalesQuery(LocalDate startDate, LocalDate endDate, Long productId,
                           String keyword, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("分页参数不正确");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.productId = productId;
        this.keyword = text(keyword);
        this.page = page;
        this.pageSize = pageSize;
    }

    public StoreSalesQuery(LocalDate startDate, LocalDate endDate, String keyword,
                           long productId, int page, int pageSize) {
        this(startDate, endDate, Long.valueOf(productId), keyword, page, pageSize);
    }

    public StoreSalesQuery(LocalDate startDate, LocalDate endDate, String keyword,
                           int page, int pageSize) {
        this(startDate, endDate, (Long) null, keyword, page, pageSize);
    }

    public StoreSalesQuery(int page, int pageSize, LocalDate startDate, LocalDate endDate,
                           Long productId, String keyword) {
        this(startDate, endDate, productId, keyword, page, pageSize);
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getFromDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getToDate() { return endDate; }
    public Long getProductId() { return productId; }
    public String getKeyword() { return keyword; }
    public String getProductKeyword() { return keyword; }
    public int getPage() { return page; }
    public int getPageNumber() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
