package edu.seu.vcampus.common.dto.identity;

import java.util.List;

/** 登录审计分页结果。 */
public final class LoginAuditPage extends IdentityPage<LoginAuditDto> {
    private static final long serialVersionUID = 1L;

    public LoginAuditPage(List<LoginAuditDto> items, int page, int pageSize, long total) {
        super(items, page, pageSize, total);
    }

    public LoginAuditPage(int page, int pageSize, long total, List<LoginAuditDto> items) {
        this(items, page, pageSize, total);
    }

    public List<LoginAuditDto> getAudits() { return getItems(); }
}
