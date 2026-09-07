package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRepairStatus;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;

import javax.swing.JButton;

/** 宿管员报修查询和受理、处理中、完成、取消状态流转。 */
public final class DormManagerRepairsPanel extends javax.swing.JPanel {
    private final BasePage page; private final DormClientService service; private final AsyncPagedTable<RepairOrderDto> repairs;

    public DormManagerRepairsPanel(BasePage page, DormClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); this.page = page; this.service = service; repairs = table(); add(repairs);
    }

    public void reload() { repairs.reload(); }

    private AsyncPagedTable<RepairOrderDto> table() {
        AsyncPagedTable<RepairOrderDto> table = new AsyncPagedTable<RepairOrderDto>("报修工单", "查看并处理报修工单。", "搜索类别、房间或描述", new String[]{"全部状态", "已提交", "已受理", "处理中", "已完成", "已取消"}, new String[]{"编号", "房间", "报修人", "类别", "描述", "优先级", "状态"}, new AsyncPagedTable.Loader<RepairOrderDto>() {
            @Override public PageSlice<RepairOrderDto> load(int p, String k, String f) throws Exception { return slice(service.repairs(new DormPageQuery(p, 20, k, repairStatus(f), null, null))); }
        }, new AsyncPagedTable.RowMapper<RepairOrderDto>() {
            @Override public Object[] values(RepairOrderDto row) { return new Object[]{row.getId(), row.getRoomId(), row.getReporterId(), RealUi.status(row.getCategory()), RealUi.text(row.getDescription()), RealUi.status(row.getPriority()), RealUi.status(row.getStatus())}; }
        }, null);
        JButton accept = new PrimaryButton("受理"); accept.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { update(DormRepairStatus.ACCEPTED, false); } }); JButton progress = new SecondaryButton("开始处理"); progress.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { update(DormRepairStatus.IN_PROGRESS, false); } }); JButton complete = new PrimaryButton("标记完成"); complete.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { update(DormRepairStatus.COMPLETED, false); } }); JButton cancel = new DangerButton("取消工单"); cancel.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { update(DormRepairStatus.CANCELLED, true); } }); table.addAction(accept); table.addAction(progress); table.addAction(complete); table.addAction(cancel); return table;
    }

    private void update(final DormRepairStatus target, boolean confirm) {
        final RepairOrderDto value = repairs.selectedItem(); if (value == null) { page.showWarning("请先选择报修工单。"); return; }
        if (confirm && !RealUi.confirm(this, "确认取消该报修工单？")) return;
        AsyncTask.run(new AsyncTask.Work<RepairOrderDto>() { @Override public RepairOrderDto run() throws Exception { return service.updateRepair(new RepairStatusRequest(value.getId(), target.name())); } }, new AsyncTask.Callback<RepairOrderDto>() { @Override public void onSuccess(RepairOrderDto result) { page.showSuccess("报修状态已更新。"); repairs.reload(); } @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); } });
    }

    private static PageSlice<RepairOrderDto> slice(DormPage<RepairOrderDto> value) { return RealUi.page(value); }
    private static String repairStatus(String filter) { if ("已提交".equals(filter)) return DormRepairStatus.SUBMITTED.name(); if ("已受理".equals(filter)) return DormRepairStatus.ACCEPTED.name(); if ("处理中".equals(filter)) return DormRepairStatus.IN_PROGRESS.name(); if ("已完成".equals(filter)) return DormRepairStatus.COMPLETED.name(); if ("已取消".equals(filter)) return DormRepairStatus.CANCELLED.name(); return null; }
}
