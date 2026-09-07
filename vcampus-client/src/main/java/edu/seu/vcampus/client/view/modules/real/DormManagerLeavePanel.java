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
import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveReviewRequest;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import org.threeten.bp.LocalDate;

/** 宿管员请假分页筛选和待审核申请审批。 */
public final class DormManagerLeavePanel extends JPanel {
    private final BasePage page;
    private final DormClientService service;
    private final JComboBox<String> status = new JComboBox<String>(
            new String[]{"全部状态", "待审批", "已通过", "已驳回", "已取消"});
    private final JTextField studentId = UiFactory.textField(8);
    private final JTextField startDate = UiFactory.textField(10);
    private final JTextField endDate = UiFactory.textField(10);
    private final JTextField remark = UiFactory.textField(20);
    private final JLabel error = UiFactory.muted(" ");
    private final JLabel selected = UiFactory.muted("选择待审批记录后操作。");
    private final AsyncPagedTable<LeaveRequestDto> leaves;

    public DormManagerLeavePanel(BasePage page, DormClientService service) {
        if (page == null || service == null) throw new IllegalArgumentException("请假审批依赖不能为空");
        this.page = page; this.service = service;
        setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        status.setFont(DesignTokens.regular(13));
        leaves = table(); add(filters()); add(leaves); add(review());
    }

    public void reload() { leaves.reload(); }

    private JPanel filters() {
        SectionCard card = new SectionCard("请假审批筛选", "按状态、学生编号和日期筛选；日期为自然日闭区间。");
        JPanel fields = new JPanel(new GridLayout(0, 4, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("状态", status));
        fields.add(UiFactory.labelledField("学生编号（筛选）", studentId));
        fields.add(UiFactory.labelledField("开始日期", startDate));
        fields.add(UiFactory.labelledField("结束日期", endDate));
        JPanel actions = UiFactory.horizontal(8);
        JButton query = new PrimaryButton("查询申请"); query.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { reload(); } });
        JButton clear = new SecondaryButton("清空筛选"); clear.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { clearFilters(); } });
        actions.add(query); actions.add(clear); actions.add(error);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER); content.add(actions, BorderLayout.SOUTH); card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); return wrap;
    }

    private AsyncPagedTable<LeaveRequestDto> table() {
        return new AsyncPagedTable<LeaveRequestDto>("请假申请列表", "处理待审批申请；学生编号仅用于筛选。",
                "上方条件优先", new String[0],
                new String[]{"编号", "学生", "类型", "开始", "结束", "原因", "状态", "审核备注"},
                new AsyncPagedTable.Loader<LeaveRequestDto>() {
                    @Override public PageSlice<LeaveRequestDto> load(int p, String keyword, String filter) throws Exception { return slice(service.managerLeaves(query(p))); }
                }, new AsyncPagedTable.RowMapper<LeaveRequestDto>() {
                    @Override public Object[] values(LeaveRequestDto row) { return RealUi.leaveRow(row, true); }
                }, new AsyncPagedTable.SelectionListener<LeaveRequestDto>() {
                    @Override public void onSelected(LeaveRequestDto row) { selectionChanged(row); }
                });
    }

    private JPanel review() {
        SectionCard card = new SectionCard("审批处理", "填写审批备注；批准或驳回需确认。");
        JPanel line = UiFactory.horizontal(8); line.add(UiFactory.body("审核备注")); line.add(remark);
        JButton approve = new PrimaryButton("批准"); approve.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(true); } });
        JButton reject = new DangerButton("驳回"); reject.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(false); } });
        line.add(approve); line.add(reject); line.add(selected);
        card.setContent(line); JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); return wrap;
    }

    private LeaveQuery query(int pageNumber) {
        try {
            Long student = RealUi.number(studentId.getText());
            if (student == null && studentId.getText().trim().length() > 0) throw new IllegalArgumentException("学生编号必须是整数");
            LocalDate start = date(startDate.getText(), "开始日期"); LocalDate end = date(endDate.getText(), "结束日期");
            if (start != null && end != null && end.isBefore(start)) throw new IllegalArgumentException("结束日期不能早于开始日期");
            error.setText(" ");
            return new LeaveQuery(pageNumber, 20, status((String) status.getSelectedItem()), student, start, end);
        } catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); throw ex; }
    }

    private void review(final boolean approved) {
        final LeaveRequestDto value = leaves.selectedItem();
        if (value == null) { error.setText("请先选择请假申请。"); return; }
        if (!"PENDING".equalsIgnoreCase(value.getStatus())) { error.setText("只有待审批申请可以处理。"); return; }
        if (!RealUi.confirm(this, approved ? "确认批准这条请假申请？" : "确认驳回这条请假申请？")) return;
        final boolean finalApproved = approved;
        AsyncTask.run(new AsyncTask.Work<LeaveRequestDto>() {
            @Override public LeaveRequestDto run() throws Exception { return service.reviewLeave(new LeaveReviewRequest(value.getId(), finalApproved,
                    RealUi.optional(remark.getText()))); }
        }, new AsyncTask.Callback<LeaveRequestDto>() {
            @Override public void onSuccess(LeaveRequestDto result) { page.showSuccess(approved ? "请假申请已批准。" : "请假申请已驳回。"); error.setText(" "); leaves.reload(); }
            @Override public void onFailure(Throwable cause) { error.setText(AsyncTask.message(cause)); }
        });
    }

    private void clearFilters() { status.setSelectedIndex(0); studentId.setText(""); startDate.setText(""); endDate.setText(""); error.setText(" "); leaves.reload(); }
    private void selectionChanged(LeaveRequestDto value) { selected.setText(value == null ? "选择待审批记录后操作。" : "已选择申请 #" + value.getId() + "（" + RealUi.status(value.getStatus()) + "）。"); }
    private static LocalDate date(String value, String label) { String text = RealUi.optional(value); if (text == null) return null; try { return LocalDate.parse(text); } catch (RuntimeException ex) { throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd"); } }
    private static PageSlice<LeaveRequestDto> slice(DormPage<LeaveRequestDto> value) { return RealUi.page(value); }
    private static String status(String value) { if ("待审批".equals(value)) return "PENDING"; if ("已通过".equals(value)) return "APPROVED"; if ("已驳回".equals(value)) return "REJECTED"; if ("已取消".equals(value)) return "CANCELLED"; return null; }
}
