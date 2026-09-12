package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.identity.IdentityClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/** 账号注销申请：本人提交/撤回，系统管理员分页审批。 */
public final class IdentityCancellationPanel extends JPanel {
    private final BasePage page;
    private final IdentityClientService service;
    private final boolean admin;
    private final AsyncPagedTable<AccountCancellationDto> table;
    private final JTextArea reason = UiFactory.textArea(3, 28);
    private final JTextField remark = UiFactory.textField(24);
    private long selectedId;

    public IdentityCancellationPanel(BasePage page, IdentityClientService service) {
        this(page, service, false);
    }

    public IdentityCancellationPanel(BasePage page, IdentityClientService service, boolean admin) {
        super();
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.admin = admin;
        table = createTable();
        add(table);
        add(admin ? adminActions() : ownActions());
    }

    private AsyncPagedTable<AccountCancellationDto> createTable() {
        String title = admin ? "账号注销审批" : "账号注销申请";
        String subtitle = "";
        String[] columns = admin ? new String[]{"账号", "原因", "状态", "提交时间"}
                : new String[]{"原因", "状态", "审核意见", "提交时间", "更新时间"};
        return new AsyncPagedTable<AccountCancellationDto>(title, subtitle, "按状态筛选",
                new String[]{"全部状态", "待审批", "已通过", "已驳回", "已撤回"}, columns,
                new AsyncPagedTable.Loader<AccountCancellationDto>() {
                    @Override public PageSlice<AccountCancellationDto> load(int p, String keyword, String filter) throws Exception {
                        return RealUi.page(admin ? service.searchAccountCancellationRequests(query(p, filter))
                                : service.listOwnAccountCancellations(query(p, filter)));
                    }
                }, new AsyncPagedTable.RowMapper<AccountCancellationDto>() {
                    @Override public Object[] values(AccountCancellationDto row) { return admin
                            ? new Object[]{RealUi.text(row.getAccount()), RealUi.text(row.getReason()), displayStatus(row.getStatus()), RealUi.dateTime(row.getCreatedAt())}
                            : new Object[]{RealUi.text(row.getReason()), displayStatus(row.getStatus()), RealUi.text(row.getReviewRemark()),
                            RealUi.dateTime(row.getCreatedAt()), RealUi.dateTime(row.getUpdatedAt())}; }
                }, new AsyncPagedTable.SelectionListener<AccountCancellationDto>() {
                    @Override public void onSelected(AccountCancellationDto row) { select(row); }
                });
    }

    private SectionCard ownActions() {
        SectionCard card = new SectionCard("提交注销申请", "");
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(UiFactory.labelledField("申请原因", reason), BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8);
        JButton submit = new PrimaryButton("提交申请");
        submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        });
        JButton withdraw = new DangerButton("撤回申请");
        withdraw.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { withdraw(); }
        });
        actions.add(submit);
        actions.add(withdraw);
        body.add(actions, BorderLayout.SOUTH);
        card.setContent(body);
        return card;
    }

    private SectionCard adminActions() {
        SectionCard card = new SectionCard("注销申请处理", "");
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(UiFactory.labelledField("审核意见", remark), BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8);
        JButton approve = new PrimaryButton("批准注销");
        approve.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(true); }
        });
        JButton reject = new DangerButton("驳回申请");
        reject.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(false); }
        });
        actions.add(approve);
        actions.add(reject);
        body.add(actions, BorderLayout.SOUTH);
        card.setContent(body);
        return card;
    }

    private AccountCancellationQuery query(int pageNumber, String filter) {
        return new AccountCancellationQuery(status(filter), pageNumber, 20);
    }

    private void select(AccountCancellationDto value) {
        selectedId = value == null ? 0L : value.getRequestId();
    }

    private void submit() {
        String value = reason.getText() == null ? "" : reason.getText().trim();
        if (value.isEmpty()) { page.showWarning("请填写注销原因。"); return; }
        if (!RealUi.confirm(this, "确认提交账号注销申请？提交后请等待管理员审核。")) return;
        final AccountCancellationRequest request = new AccountCancellationRequest(value);
        AsyncTask.run(new AsyncTask.Work<AccountCancellationDto>() {
                    @Override public AccountCancellationDto run() throws Exception { return service.submitAccountCancellation(request); }
                },
                callback("账号注销申请已提交。", true));
    }

    private void withdraw() {
        if (!selected()) return;
        if (!RealUi.confirm(this, "确认撤回当前选中的注销申请？")) return;
        final long requestId = selectedId;
        AsyncTask.run(new AsyncTask.Work<AccountCancellationDto>() {
                    @Override public AccountCancellationDto run() throws Exception { return service.withdrawAccountCancellation(requestId); }
                },
                callback("注销申请已撤回。", false));
    }

    private void review(boolean approve) {
        if (!selected()) return;
        String action = approve ? "批准" : "驳回";
        if (!RealUi.confirm(this, "确认" + action + "这条账号注销申请？")) return;
        final AccountCancellationReviewRequest request = new AccountCancellationReviewRequest(selectedId,
                remark.getText() == null ? null : remark.getText().trim());
        final boolean accepted = approve;
        AsyncTask.run(new AsyncTask.Work<AccountCancellationDto>() {
                    @Override public AccountCancellationDto run() throws Exception { return accepted ? service.approveAccountCancellation(request)
                            : service.rejectAccountCancellation(request); }
                }, callback("申请已" + action + "。", false));
    }

    private AsyncTask.Callback<AccountCancellationDto> callback(final String message, final boolean clearReason) {
        return new AsyncTask.Callback<AccountCancellationDto>() {
            @Override public void onSuccess(AccountCancellationDto value) {
                if (clearReason) reason.setText("");
                remark.setText("");
                selectedId = 0L;
                page.showSuccess(message);
                table.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        };
    }

    private boolean selected() {
        if (selectedId > 0L) return true;
        page.showWarning("请先选择申请记录。");
        return false;
    }

    private static String status(String filter) {
        if ("待审批".equals(filter)) return "PENDING";
        if ("已通过".equals(filter)) return "APPROVED";
        if ("已驳回".equals(filter)) return "REJECTED";
        return "已撤回".equals(filter) ? "CANCELLED" : null;
    }

    private static String displayStatus(String value) {
        return "CANCELLED".equalsIgnoreCase(value) ? "已撤回" : RealUi.status(value);
    }
}
