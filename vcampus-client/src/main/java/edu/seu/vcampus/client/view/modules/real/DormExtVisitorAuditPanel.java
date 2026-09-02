package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorAuditRequest;
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationDto;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** 宿管员来访登记审核。 */
public final class DormExtVisitorAuditPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormExtClientService service;
    private final AsyncPagedTable<VisitorRegistrationDto> registrations;
    private final JTextField remark = UiFactory.textField(20);

    public DormExtVisitorAuditPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.registrations = table();
        add(registrations);
        add(actions());
    }

    public void reload() { registrations.reload(); }

    private AsyncPagedTable<VisitorRegistrationDto> table() {
        return new AsyncPagedTable<VisitorRegistrationDto>("来访登记审核",
                "证件号只显示掩码；线下核验请与来访人当面核对。",
                "搜索来访人、事由或房间",
                new String[]{"全部状态", "待审核", "已通过", "已驳回", "已撤销"},
                new String[]{"编号", "学生", "房间", "来访人", "证件号", "事由", "来访时间", "离开时间", "状态"},
                new AsyncPagedTable.Loader<VisitorRegistrationDto>() {
                    @Override
                    public PageSlice<VisitorRegistrationDto> load(int p, String keyword, String filter)
                            throws Exception {
                        DormPage<VisitorRegistrationDto> value = service.visitors(
                                new DormPageQuery(p, 20, keyword, DormExtVisitorLabels.code(filter),
                                        null, null));
                        return RealUi.page(value);
                    }
                },
                new AsyncPagedTable.RowMapper<VisitorRegistrationDto>() {
                    @Override public Object[] values(VisitorRegistrationDto row) {
                        return DormExtVisitorLabels.row(row, true);
                    }
                }, null);
    }

    private JPanel actions() {
        SectionCard card = new SectionCard("审核", "只有待审核的登记可以处理。");
        JPanel line = UiFactory.horizontal(8);
        line.add(UiFactory.body("审核意见"));
        line.add(remark);
        JButton approve = new PrimaryButton("通过");
        approve.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { audit(true); }
        });
        JButton reject = new SecondaryButton("驳回");
        reject.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { audit(false); }
        });
        line.add(approve);
        line.add(reject);
        card.setContent(line);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void audit(final boolean approved) {
        final VisitorRegistrationDto selected = registrations.selectedItem();
        if (selected == null) { page.showWarning("请先选择一条登记。"); return; }
        final VisitorAuditRequest request = new VisitorAuditRequest(selected.getId(), approved,
                RealUi.optional(remark.getText()));
        AsyncTask.run(new AsyncTask.Work<VisitorRegistrationDto>() {
            @Override public VisitorRegistrationDto run() throws Exception {
                return service.auditVisitor(request);
            }
        }, new AsyncTask.Callback<VisitorRegistrationDto>() {
            @Override public void onSuccess(VisitorRegistrationDto value) {
                page.showSuccess(approved ? "登记已通过。" : "登记已驳回。");
                remark.setText("");
                registrations.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
}
