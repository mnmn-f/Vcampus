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
import edu.seu.vcampus.common.dto.dorm.ext.VisitorRegistrationRequest;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 学生端外来人员来访登记。
 *
 * <p>表单里没有房间字段：来访房间由服务端按本人在住记录解析，学生登记不到别人
 * 的宿舍上。证件号提交后不再回传，列表里只显示掩码。</p>
 */
public final class DormExtVisitorPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormExtClientService service;
    private final AsyncPagedTable<VisitorRegistrationDto> registrations;

    private final JTextField visitorName = UiFactory.textField(10);
    private final JTextField visitorIdCard = UiFactory.textField(18);
    private final JTextField visitorPhone = UiFactory.textField(12);
    private final JTextField visitReason = UiFactory.textField(16);
    private final JTextField startAt = UiFactory.textField(16);
    private final JTextField endAt = UiFactory.textField(16);

    public DormExtVisitorPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.registrations = table();
        add(form());
        add(registrations);
        add(actions());
    }

    public void reload() { registrations.reload(); }

    private JPanel form() {
        SectionCard card = new SectionCard("外来人员登记",
                "来访房间按你的住宿记录自动确定；证件号仅供宿管线下核验，登记后只显示掩码。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("来访人姓名", visitorName));
        fields.add(UiFactory.labelledField("来访人证件号", visitorIdCard));
        fields.add(UiFactory.labelledField("联系电话（可空）", visitorPhone));
        fields.add(UiFactory.labelledField("来访事由", visitReason));
        fields.add(UiFactory.labelledField("来访时间（yyyy-MM-dd HH:mm）", startAt));
        fields.add(UiFactory.labelledField("离开时间（yyyy-MM-dd HH:mm）", endAt));

        JPanel line = UiFactory.horizontal(8);
        JButton reset = new SecondaryButton("清空");
        reset.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { clearForm(); }
        });
        JButton submit = new PrimaryButton("提交登记");
        submit.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { submit(); }
        });
        line.add(reset);
        line.add(submit);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(line, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private AsyncPagedTable<VisitorRegistrationDto> table() {
        return new AsyncPagedTable<VisitorRegistrationDto>("我的来访登记",
                "仅展示你提交的记录。", "搜索来访人或事由",
                new String[]{"全部状态", "待审核", "已通过", "已驳回", "已撤销"},
                new String[]{"编号", "来访人", "证件号", "事由", "来访时间", "离开时间", "状态", "审核意见"},
                new AsyncPagedTable.Loader<VisitorRegistrationDto>() {
                    @Override
                    public PageSlice<VisitorRegistrationDto> load(int p, String keyword, String filter)
                            throws Exception {
                        DormPage<VisitorRegistrationDto> value = service.ownVisitors(
                                new DormPageQuery(p, 20, keyword, DormExtVisitorLabels.code(filter),
                                        null, null));
                        return RealUi.page(value);
                    }
                },
                new AsyncPagedTable.RowMapper<VisitorRegistrationDto>() {
                    @Override public Object[] values(VisitorRegistrationDto row) {
                        return DormExtVisitorLabels.row(row, false);
                    }
                }, null);
    }

    private JPanel actions() {
        SectionCard card = new SectionCard("撤销登记", "只有待审核的登记可以撤销。");
        JPanel line = UiFactory.horizontal(8);
        JButton cancel = new SecondaryButton("撤销选中登记");
        cancel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { cancel(); }
        });
        line.add(cancel);
        card.setContent(line);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void clearForm() {
        visitorName.setText("");
        visitorIdCard.setText("");
        visitorPhone.setText("");
        visitReason.setText("");
        startAt.setText("");
        endAt.setText("");
    }

    private void submit() {
        final VisitorRegistrationRequest request;
        try {
            request = new VisitorRegistrationRequest(
                    RealUi.required(visitorName.getText(), "来访人姓名"),
                    RealUi.required(visitorIdCard.getText(), "来访人证件号"),
                    RealUi.optional(visitorPhone.getText()),
                    RealUi.required(visitReason.getText(), "来访事由"),
                    DormExtVisitorLabels.dateTime(startAt.getText(), "来访时间"),
                    DormExtVisitorLabels.dateTime(endAt.getText(), "离开时间"));
        } catch (IllegalArgumentException ex) {
            page.showWarning(ex.getMessage());
            return;
        }
        AsyncTask.run(new AsyncTask.Work<VisitorRegistrationDto>() {
            @Override public VisitorRegistrationDto run() throws Exception {
                return service.submitVisitor(request);
            }
        }, new AsyncTask.Callback<VisitorRegistrationDto>() {
            @Override public void onSuccess(VisitorRegistrationDto value) {
                page.showSuccess("登记已提交，等待宿管审核。");
                clearForm();
                registrations.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void cancel() {
        final VisitorRegistrationDto selected = registrations.selectedItem();
        if (selected == null) { page.showWarning("请先选择一条登记。"); return; }
        AsyncTask.run(new AsyncTask.Work<VisitorRegistrationDto>() {
            @Override public VisitorRegistrationDto run() throws Exception {
                return service.cancelVisitor(new VisitorAuditRequest(selected.getId(), false, null));
            }
        }, new AsyncTask.Callback<VisitorRegistrationDto>() {
            @Override public void onSuccess(VisitorRegistrationDto value) {
                page.showSuccess("登记已撤销。");
                registrations.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
}
