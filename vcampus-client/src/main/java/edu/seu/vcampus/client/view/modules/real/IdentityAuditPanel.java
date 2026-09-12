package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.BusinessAuditDto;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditDto;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;

import javax.swing.JPanel;

/** 系统管理员的登录和业务审计只读列表。 */
public final class IdentityAuditPanel extends JPanel {
    private final IdentityClientService service;

    public IdentityAuditPanel(BasePage page, IdentityClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); this.service = service;
        add(loginTable()); add(businessTable());
    }

    private AsyncPagedTable<LoginAuditDto> loginTable() {
        return new AsyncPagedTable<LoginAuditDto>("登录审计", "", null,
                new String[]{"全部结果", "成功", "凭据错误"}, new String[]{"时间", "账号", "身份", "结果", "来源"},
                new AsyncPagedTable.Loader<LoginAuditDto>() {
                    @Override public PageSlice<LoginAuditDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.searchLoginAudits(new AuditQuery(null, null, loginOutcome(filter), p, 20)));
                    }
                }, new AsyncPagedTable.RowMapper<LoginAuditDto>() {
                    @Override public Object[] values(LoginAuditDto row) { return new Object[]{RealUi.dateTime(row.getOccurredAt()), row.getUsernameSnapshot(),
                            row.getRole() == null ? "--" : row.getRole().getDisplayName(), RealUi.status(row.getResultCode()), RealUi.text(row.getClientIp())}; }
                }, null);
    }

    private AsyncPagedTable<BusinessAuditDto> businessTable() {
        return new AsyncPagedTable<BusinessAuditDto>("业务审计", "", "搜索操作",
                new String[]{"全部结果", "成功", "失败"}, new String[]{"时间", "身份", "操作", "资源", "结果"},
                new AsyncPagedTable.Loader<BusinessAuditDto>() {
                    @Override public PageSlice<BusinessAuditDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.searchBusinessAudits(new AuditQuery(null, RealUi.optional(keyword), businessOutcome(filter), p, 20)));
                    }
                }, new AsyncPagedTable.RowMapper<BusinessAuditDto>() {
                    @Override public Object[] values(BusinessAuditDto row) { return new Object[]{RealUi.dateTime(row.getOccurredAt()),
                            row.getActorRole() == null ? "--" : row.getActorRole().getDisplayName(), RealUi.text(row.getAction()),
                            RealUi.text(row.getResourceType()), RealUi.status(row.getOutcome())}; }
                }, null);
    }

    private static PageSlice<LoginAuditDto> slice(LoginAuditPage page) { return RealUi.page(page); }
    private static PageSlice<BusinessAuditDto> slice(BusinessAuditPage page) { return RealUi.page(page); }
    private static String loginOutcome(String filter) { return "成功".equals(filter) ? "OK" : "凭据错误".equals(filter) ? "AUTH.INVALID_CREDENTIALS" : null; }
    private static String businessOutcome(String filter) { return "成功".equals(filter) ? "SUCCESS" : "失败".equals(filter) ? "FAILURE" : null; }
}
