package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountRechargeRequest;
import edu.seu.vcampus.common.dto.store.AccountTransactionDto;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;

/** 学生校园账户余额、流水和充值。 */
public final class StoreAccountPanel extends JPanel {
    private final BasePage page;
    private final StoreClientService service;
    private final JLabel balance = UiFactory.title("¥--");
    private final JLabel status = UiFactory.muted("账户加载中…");
    private final JTextField amount = UiFactory.textField(10);
    private final JTextField remark = UiFactory.textField(12);
    private final AsyncPagedTable<AccountTransactionDto> ledger;
    private String rechargeKey;
    private int accountSerial;

    public StoreAccountPanel(BasePage page, StoreClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service;
        add(summary()); ledger = ledger(); add(ledger); loadAccount();
        edu.seu.vcampus.client.ui.VisibleRefresh.attach(this, () -> true, this::loadAccount);
    }

    public void reload() { loadAccount(); ledger.reload(); }

    private JPanel summary() {
        JPanel panel = new JPanel(new BorderLayout(16, 0)); panel.setOpaque(false);
        SectionCard card = new SectionCard("校园账户", "余额、充值和账户流水。");
        JPanel line = UiFactory.horizontal(12);
        line.add(balance); line.add(status); line.add(UiFactory.body("充值金额")); line.add(amount);
        line.add(UiFactory.body("备注")); line.add(remark);
        JButton recharge = new PrimaryButton("充值"); recharge.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { recharge(); }
        }); line.add(recharge);
        card.setContent(line); panel.add(card, BorderLayout.CENTER); return panel;
    }

    private AsyncPagedTable<AccountTransactionDto> ledger() {
        return new AsyncPagedTable<AccountTransactionDto>("账户流水", "",
                "按类型筛选", new String[]{"全部流水", "充值", "消费", "退款"},
                new String[]{"流水号", "类型", "金额", "余额后", "关联业务", "时间"},
                new AsyncPagedTable.Loader<AccountTransactionDto>() {
                    @Override public PageSlice<AccountTransactionDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.getAccountLedger(new AccountLedgerQuery(type(filter), p, 20)));
                    }
                }, new AsyncPagedTable.RowMapper<AccountTransactionDto>() {
                    @Override public Object[] values(AccountTransactionDto row) { return new Object[]{row.getId(), RealUi.status(row.getTransactionType()),
                            money(row.getAmount()), money(row.getBalanceAfter()), RealUi.text(row.getReferenceType()), RealUi.dateTime(row.getCreatedAt())}; }
                }, null);
    }

    private void loadAccount() {
        final int serial = ++accountSerial;
        status.setText("账户加载中…");
        AsyncTask.run(new AsyncTask.Work<AccountDto>() {
            @Override public AccountDto run() throws Exception { return service.getAccount(); }
        }, new AsyncTask.Callback<AccountDto>() {
            @Override public void onSuccess(AccountDto value) { if (serial != accountSerial) return; balance.setText("¥" + moneyValue(value.getBalance())); status.setText("状态：" + RealUi.status(value.getStatus())); }
            @Override public void onFailure(Throwable error) { if (serial != accountSerial) return; status.setText("账户加载失败：" + AsyncTask.message(error)); }
        });
    }

    private void recharge() {
        try {
            final BigDecimal value = new BigDecimal(RealUi.required(amount.getText(), "充值金额"));
            if (value.signum() <= 0) throw new IllegalArgumentException("充值金额必须大于 0");
            if (!StoreRechargeQrDialog.show(this, value)) return;
            if (rechargeKey == null) rechargeKey = "desktop-recharge-" + System.currentTimeMillis();
            final AccountRechargeRequest request = new AccountRechargeRequest(value, rechargeKey, RealUi.optional(remark.getText()));
            AsyncTask.run(new AsyncTask.Work<AccountDto>() {
                @Override public AccountDto run() throws Exception { return service.recharge(request); }
            }, new AsyncTask.Callback<AccountDto>() {
                @Override public void onSuccess(AccountDto result) { page.showSuccess("充值成功，当前余额 ¥" + moneyValue(result.getBalance())); rechargeKey = null; amount.setText(""); loadAccount(); ledger.reload(); }
                @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
            });
        } catch (NumberFormatException ex) { page.showWarning("充值金额格式不正确。"); }
        catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); }
    }

    private static PageSlice<AccountTransactionDto> slice(AccountLedgerPage value) { return new PageSlice<AccountTransactionDto>(value.getItems(), value.getTotal(), value.getPage(), value.getPageSize()); }
    private static String type(String value) { if ("充值".equals(value)) return "RECHARGE"; if ("消费".equals(value)) return "PURCHASE"; if ("退款".equals(value)) return "REFUND"; return null; }
    private static String money(BigDecimal value) { return value == null ? "--" : (value.signum() >= 0 ? "+¥" : "-¥") + moneyValue(value.abs()); }
    private static String moneyValue(BigDecimal value) { return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
}
