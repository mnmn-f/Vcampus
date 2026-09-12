package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
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
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import org.threeten.bp.LocalDateTime;

/** 学生本人请假提交、历史查询和待审核申请撤回。 */
public final class DormStudentLeavePanel extends JPanel {
    private final BasePage page;
    private final DormClientService service;
    private final JComboBox<RealUi.CodeOption> type = new JComboBox<RealUi.CodeOption>(
            RealUi.options("PERSONAL", "ILLNESS", "OFF_CAMPUS", "OTHER"));
    // 离校只需要选到「哪天」：请假是按天算的，让人再拨一次时分只是多两步操作。
    // 服务端要的是时刻，所以开始取当天 00:00、结束取当天 23:59，覆盖整天。
    private final DormDateField start = new DormDateField(10);
    private final DormDateField end = new DormDateField(10);
    private final JTextField reason = UiFactory.textField(18);
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

    /**
     * 表单排成一行，每个输入框只占它需要的宽度。
     *
     * <p>之前用两列等分网格，「请假类型」那个下拉框被撑到半个屏幕宽，右边同一行的
     * 东西反而被挤出框。输入框的宽度应该由内容决定：类型是四选一，日期是十个字符，
     * 只有事由值得占剩下的全部空间。</p>
     */
    private JPanel form() {
        type.setFont(DesignTokens.regular(15));
        type.setPreferredSize(new Dimension(112, 33));

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        fields.setOpaque(false);
        fields.add(DormFormUi.field("类型", type, 112));
        fields.add(DormFormUi.field("开始日期", start, 190));
        fields.add(DormFormUi.field("结束日期", end, 190));
        fields.add(DormFormUi.field("事由", reason, 240));

        JButton submit = new PrimaryButton("提交请假");
        submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        });
        JButton clear = new SecondaryButton("清空");
        clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { clearForm(); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(submit);
        buttons.add(clear);

        JPanel box = DormUi.panel();
        JPanel inner = new JPanel(new BorderLayout(0, 12));
        inner.setOpaque(false);
        inner.add(fields, BorderLayout.CENTER);
        JPanel foot = new JPanel(new BorderLayout(12, 0));
        foot.setOpaque(false);
        foot.add(buttons, BorderLayout.WEST);
        JPanel notes = UiFactory.horizontal(12);
        notes.add(selection);
        notes.add(error);
        foot.add(notes, BorderLayout.CENTER);
        inner.add(foot, BorderLayout.SOUTH);
        box.add(inner, BorderLayout.CENTER);
        box.setAlignmentX(LEFT_ALIGNMENT);

        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new javax.swing.BoxLayout(section, javax.swing.BoxLayout.Y_AXIS));
        section.add(DormUi.header("离校请假", "请假期间不计入连续未归预警；结束日期必须不早于开始日期。", null, false));
        section.add(box);
        section.add(javax.swing.Box.createVerticalStrut(20));
        section.setAlignmentX(LEFT_ALIGNMENT);
        return section;
    }

    private AsyncPagedTable<LeaveRequestDto> history() {
        return new AsyncPagedTable<LeaveRequestDto>("我的请假记录", "仅显示本人申请。",
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
            // 开始取当天零点、结束取当天 23:59，这样「9-20 到 9-20」就是完整的一天，
            // 而不是一个长度为零、会被服务端判非法的区间。
            final LocalDateTime from = start.required("开始日期").atStartOfDay();
            final LocalDateTime to = end.required("结束日期").atTime(23, 59);
            if (!to.isAfter(from)) throw new IllegalArgumentException("结束日期不能早于开始日期");
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

    private void clearForm() { start.clear(); end.clear(); reason.setText(""); showError(" "); }
    private void showError(String text) { error.setText(RealUi.text(text)); }
    private static PageSlice<LeaveRequestDto> slice(DormPage<LeaveRequestDto> value) { return RealUi.page(value); }
    private static String leaveStatus(String value) { if ("待审批".equals(value)) return "PENDING"; if ("已通过".equals(value)) return "APPROVED"; if ("已驳回".equals(value)) return "REJECTED"; if ("已取消".equals(value)) return "CANCELLED"; return null; }
}
