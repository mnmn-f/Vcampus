package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

/** 学生对本人已完成且未评价的报修工单进行一次评价。 */
public final class DormStudentRepairEvaluationPanel extends JPanel {
    private final BasePage page;
    private final DormClientService service;
    private final JComboBox<Integer> score = new JComboBox<Integer>(new Integer[]{1, 2, 3, 4, 5});
    private final JTextArea note = UiFactory.textArea(2, 28);
    private final JLabel state = UiFactory.muted("选择已完成且未评价的工单。");
    private final JLabel error = UiFactory.muted(" ");
    private final JButton submit = new PrimaryButton("提交评价");
    private final AsyncPagedTable<RepairOrderDto> repairs;

    public DormStudentRepairEvaluationPanel(BasePage page, DormClientService service) {
        if (page == null || service == null) throw new IllegalArgumentException("报修评价依赖不能为空");
        this.page = page; this.service = service;
        setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        score.setFont(DesignTokens.regular(13)); submit.setEnabled(false); submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { evaluate(); }
        });
        repairs = table(); add(repairs); add(form());
    }

    public void reload() { repairs.reload(); }

    private AsyncPagedTable<RepairOrderDto> table() {
        return new AsyncPagedTable<RepairOrderDto>("报修评价", "本人已完成工单，每个工单只能评价一次。",
                "完成工单", new String[0],
                new String[]{"编号", "房间", "类别", "描述", "完成时间", "评分", "评价"},
                new AsyncPagedTable.Loader<RepairOrderDto>() {
                    @Override public PageSlice<RepairOrderDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.repairs(new DormPageQuery(p, 20, keyword, "COMPLETED", null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<RepairOrderDto>() {
                    @Override public Object[] values(RepairOrderDto row) { return new Object[]{row.getId(), row.getRoomId(), RealUi.status(row.getCategory()),
                            RealUi.text(row.getDescription()), RealUi.dateTime(row.getCompletedAt()), row.getEvaluationScore() == null ? "未评价" : row.getEvaluationScore(),
                            RealUi.text(row.getEvaluationNote())}; }
                }, new AsyncPagedTable.SelectionListener<RepairOrderDto>() {
                    @Override public void onSelected(RepairOrderDto row) { selectionChanged(row); }
                });
    }

    private JPanel form() {
        SectionCard card = new SectionCard("填写评价", "评分为 1–5 分；提交后不能修改。");
        JPanel line = UiFactory.horizontal(8); line.add(UiFactory.body("评分")); line.add(score);
        line.add(UiFactory.body("评价内容")); line.add(note); line.add(submit); line.add(state); line.add(error);
        card.setContent(line); JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); return wrap;
    }

    private void selectionChanged(RepairOrderDto value) {
        boolean available = value != null && "COMPLETED".equalsIgnoreCase(value.getStatus())
                && value.getEvaluationScore() == null;
        submit.setEnabled(available);
        state.setText(value == null ? "选择已完成且未评价的工单。" : available
                ? "当前工单可以评价。" : "当前工单已评价或不可评价。");
    }

    private void evaluate() {
        RepairOrderDto value = repairs.selectedItem();
        if (value == null) { error.setText("请先选择可评价工单。"); return; }
        if (value.getEvaluationScore() != null) { error.setText("该工单已经评价过。"); return; }
        final RepairEvaluationRequest request = new RepairEvaluationRequest(value.getId(),
                ((Integer) score.getSelectedItem()).intValue(), RealUi.optional(note.getText()));
        AsyncTask.run(new AsyncTask.Work<RepairOrderDto>() {
                    @Override public RepairOrderDto run() throws Exception { return service.evaluateRepair(request); }
                },
                new AsyncTask.Callback<RepairOrderDto>() {
                    @Override public void onSuccess(RepairOrderDto result) { page.showSuccess("报修评价已提交。"); note.setText(""); error.setText(" "); repairs.reload(); }
                    @Override public void onFailure(Throwable cause) { error.setText(AsyncTask.message(cause)); }
                });
    }

    private static PageSlice<RepairOrderDto> slice(DormPage<RepairOrderDto> value) { return RealUi.page(value); }
}
