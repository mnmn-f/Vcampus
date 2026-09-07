package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.LeaveCancelRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

/** 学生本人请假提交、历史查询和待审核申请撤回。 */
public final class DormStudentLeavePanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final BasePage page;
    private final DormClientService service;
    private final JComboBox<RealUi.CodeOption> type = new JComboBox<RealUi.CodeOption>(
            RealUi.options("PERSONAL", "ILLNESS", "OFF_CAMPUS", "OTHER"));
    private final JTextField start = UiFactory.textField(16);
    private final JTextField end = UiFactory.textField(16);
    private final JTextArea reason = UiFactory.textArea(2, 28);
    private final JLabel error = UiFactory.muted(" ");
    private final JLabel selection = UiFactory.muted("选择待审核记录可撤回。");
    private final JButton cancel = new DangerButton("撤回申请");
    private final AsyncPagedTable<LeaveRequestDto> history;

    public DormStudentLeavePanel(BasePage page, DormClientService service) {
        if (page == null || service == null) throw new IllegalArgumentException("请假依赖不能为空");
        this.page = page; this.service = service;
        setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        history = history();
        cancel.setEnabled(false); cancel.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cancelSelected(); }
        });
        history.addAction(cancel);
        add(form()); add(history);
    }

    public void reload() { history.reload(); }

    private JPanel form() {
        SectionCard card = new SectionCard("学生请假", "申请人自动填写为当前账号；时间格式 yyyy-MM-dd HH:mm，结束时间必须晚于开始时间。");
        type.setFont(DesignTokens.regular(13));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("请假类型", type));
        fields.add(UiFactory.labelledField("开始时间", start));
        fields.add(UiFactory.labelledField("结束时间", end));
        fields.add(UiFactory.labelledField("请假原因", reason));
        JPanel actions = UiFactory.horizontal(8);
        JButton submit = new PrimaryButton("提交请假"); submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        });
        JButton clear = new SecondaryButton("清空表单"); clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { clearForm(); }
        });
        actions.add(submit); actions.add(clear); actions.add(selection); actions.add(error);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER); content.add(actions, BorderLayout.SOUTH); card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); return wrap;
    }

    private AsyncPagedTable<LeaveRequestDto> history() {
        return new AsyncPagedTable<LeaveRequestDto>("我的请假记录", "仅显示本人申请，不能填写或筛选其他学生编号。",
                "按状态筛选", new String[]{"全部状态", "待审批", "已通过", "已驳回", "已取消"},
                new String[]{"编号", "类型", "开始", "结束", "原因", "状态", "审核备注"},
                new AsyncPagedTable.Loader<LeaveRequestDto>() {
                    @Override public PageSlice<LeaveRequestDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.myLeaves(new LeaveQuery(p, 20, leaveStatus(filter), (Long) null, null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<LeaveRequestDto>() {
                    @Override public Object[] values(LeaveRequestDto row) { return RealUi.leaveRow(row, false); }
                }, new AsyncPagedTable.SelectionListener<LeaveRequestDto>() {
                    @Override public void onSelected(LeaveRequestDto row) { selectionChanged(row); }
                });
    }

    private void submit() {
        try {
            final LocalDateTime from = dateTime(start.getText(), "开始时间");
            final LocalDateTime to = dateTime(end.getText(), "结束时间");
            if (!to.isAfter(from)) throw new IllegalArgumentException("结束时间必须晚于开始时间");
            final String code = RealUi.required(RealUi.code(type.getSelectedItem()), "请假类型");
            final LeaveSubmitRequest request = new LeaveSubmitRequest(code, from, to, RealUi.optional(reason.getText()));
            AsyncTask.run(new AsyncTask.Work<LeaveRequestDto>() {
                @Override public LeaveRequestDto run() throws Exception { return service.submitLeave(request); }
            }, new AsyncTask.Callback<LeaveRequestDto>() {
                @Override public void onSuccess(LeaveRequestDto value) { page.showSuccess("请假申请已提交。"); clearForm(); history.reload(); }
                @Override public void onFailure(Throwable cause) { showError(AsyncTask.message(cause)); }
            });
        } catch (IllegalArgumentException ex) { showError(ex.getMessage()); }
    }

    private void cancelSelected() {
        LeaveRequestDto value = history.selectedItem();
        if (value == null) { showError("请先选择请假申请。"); return; }
        if (!"PENDING".equalsIgnoreCase(value.getStatus())) { showError("只有待审批申请可以撤回。"); return; }
        if (!RealUi.confirm(this, "确认撤回这条请假申请？")) return;
        final long requestId = value.getId();
        AsyncTask.run(new AsyncTask.Work<LeaveRequestDto>() {
                    @Override public LeaveRequestDto run() throws Exception { return service.cancelLeave(new LeaveCancelRequest(requestId)); }
                },
                new AsyncTask.Callback<LeaveRequestDto>() {
                    @Override public void onSuccess(LeaveRequestDto result) { page.showSuccess("请假申请已撤回。"); history.reload(); }
                    @Override public void onFailure(Throwable cause) { showError(AsyncTask.message(cause)); }
                });
    }

    private void selectionChanged(LeaveRequestDto value) {
        boolean pending = value != null && "PENDING".equalsIgnoreCase(value.getStatus());
        cancel.setEnabled(pending); selection.setText(value == null ? "选择待审核记录可撤回。"
                : pending ? "当前记录可撤回。" : "当前记录已处理，不能撤回。");
    }

    private void clearForm() { start.setText(""); end.setText(""); reason.setText(""); showError(" "); }
    private void showError(String text) { error.setText(RealUi.text(text)); }
    private static LocalDateTime dateTime(String value, String label) {
        try { return LocalDateTime.parse(RealUi.required(value, label), DATE_TIME); }
        catch (org.threeten.bp.format.DateTimeParseException ex) { throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd HH:mm"); }
    }
    private static PageSlice<LeaveRequestDto> slice(DormPage<LeaveRequestDto> value) { return RealUi.page(value); }
    private static String leaveStatus(String value) { if ("待审批".equals(value)) return "PENDING"; if ("已通过".equals(value)) return "APPROVED"; if ("已驳回".equals(value)) return "REJECTED"; if ("已取消".equals(value)) return "CANCELLED"; return null; }
}
