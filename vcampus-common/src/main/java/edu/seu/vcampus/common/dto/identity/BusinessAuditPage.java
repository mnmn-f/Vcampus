package edu.seu.vcampus.common.dto.identity;

import java.util.List;

/** 业务审计分页结果。 */
public final class BusinessAuditPage extends IdentityPage<BusinessAuditDto> {
    private static final long serialVersionUID = 1L;

    public BusinessAuditPage(List<BusinessAuditDto> items, int page, int pageSize, long total) {
        super(items, page, pageSize, total);
    }

    public BusinessAuditPage(int page, int pageSize, long total,
                             List<BusinessAuditDto> items) {
        this(items, page, pageSize, total);
    }

    public List<BusinessAuditDto> getAudits() { return getItems(); }
}
