package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** PDF 资源协议的数据快照。 */
public final class PdfQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String scope;
    private final String keyword;
    private final String status;
    private final int page;
    private final int pageSize;

    public PdfQuery(String scope, String keyword, String status, int page, int pageSize) {
        this.scope = scope;
        this.keyword = keyword;
        this.status = status;
        this.page = page;
        this.pageSize = pageSize;
    }
    public String getScope() { return scope; }
    public String getKeyword() { return keyword; }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
