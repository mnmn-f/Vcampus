package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRepairStatus;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 学生本人报修列表和行内新建报修。 */
public final class DormStudentRepairsPanel extends JPanel {
    private final BasePage page;
    private final DormClientService service;
    private final AsyncPagedTable<RepairOrderDto> repairs;
    private final JTextField roomId = UiFactory.textField(8);
    private final JTextField category = UiFactory.textField(10);
    private final JComboBox<RealUi.CodeOption> priority = new JComboBox<RealUi.CodeOption>(
            RealUi.options("NORMAL", "HIGH", "LOW"));
    private final JTextArea description = UiFactory.textArea(2, 24);

    public DormStudentRepairsPanel(BasePage page, DormClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; priority.setFont(edu.seu.vcampus.client.ui.DesignTokens.regular(13));
        repairs = table(); add(repairs); add(form());
    }

    public void reload() { repairs.reload(); }

    private AsyncPagedTable<RepairOrderDto> table() {
        return new AsyncPagedTable<RepairOrderDto>("我的报修", "仅显示本人报修记录。", "按类别或描述搜索",
                new String[]{"全部状态", "已提交", "处理中", "已完成", "已取消"},
                new String[]{"编号", "类别", "描述", "优先级", "状态", "提交时间"},
                new AsyncPagedTable.Loader<RepairOrderDto>() {
                    @Override public PageSlice<RepairOrderDto> load(int p, String keyword, String filter) throws Exception {
                        return slice(service.repairs(new DormPageQuery(p, 20, keyword, repairStatus(filter), null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<RepairOrderDto>() {
                    @Override public Object[] values(RepairOrderDto row) { return new Object[]{row.getId(), RealUi.status(row.getCategory()), RealUi.text(row.getDescription()),
                            RealUi.status(row.getPriority()), RealUi.status(row.getStatus()), RealUi.dateTime(row.getSubmittedAt())}; }
                }, null);
    }

    private JPanel form() {
        SectionCard card = new SectionCard("提交报修", "填写房间编号和问题描述。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("房间编号", roomId)); fields.add(UiFactory.labelledField("问题类别", category));
        fields.add(UiFactory.labelledField("优先级", priority)); fields.add(UiFactory.labelledField("问题描述", description));
        JButton save = new PrimaryButton("提交报修"); save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { create(); }
        });
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false); content.add(fields, BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); actions.add(save); content.add(actions, BorderLayout.SOUTH); card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); return wrap;
    }

    private void create() {
        try {
            Long room = RealUi.number(RealUi.required(roomId.getText(), "房间编号"));
            if (room == null) throw new IllegalArgumentException("房间编号必须是整数");
            final String type = RealUi.required(category.getText(), "问题类别"); final String text = RealUi.required(description.getText(), "问题描述");
            final RepairCreateRequest request = new RepairCreateRequest(room.longValue(), type, text, RealUi.code(priority.getSelectedItem()));
            AsyncTask.run(new AsyncTask.Work<RepairOrderDto>() {
                @Override public RepairOrderDto run() throws Exception { return service.createRepair(request); }
            }, new AsyncTask.Callback<RepairOrderDto>() {
                @Override public void onSuccess(RepairOrderDto value) { page.showSuccess("报修已提交。"); description.setText(""); repairs.reload(); }
                @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
            });
        } catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); }
    }

    private static PageSlice<RepairOrderDto> slice(DormPage<RepairOrderDto> value) { return RealUi.page(value); }
    private static String repairStatus(String filter) { if ("已提交".equals(filter)) return DormRepairStatus.SUBMITTED.name(); if ("处理中".equals(filter)) return DormRepairStatus.IN_PROGRESS.name(); if ("已完成".equals(filter)) return DormRepairStatus.COMPLETED.name(); if ("已取消".equals(filter)) return DormRepairStatus.CANCELLED.name(); return null; }
}
