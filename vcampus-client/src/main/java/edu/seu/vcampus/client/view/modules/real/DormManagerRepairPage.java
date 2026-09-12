package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRepairStatus;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RepairAssignRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkOrderDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkerDto;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 宿管的报修处理：左边工单表，右边把选中的这一单派给谁。
 *
 * <p>右栏跟着选中工单的状态换：还没派出去就显示维修员名单，维修员报完工就换成审核
 * 按钮。宿管在这条流水线上只有两个动作——派给谁、修得行不行；把四个状态按钮一直摆
 * 在那里只会让人误按。</p>
 *
 * <p>特别是没有「处理中」：那是维修员到场后自己点的，宿管替他点等于替他签到。之前
 * 「受理」会把处理人写成宿管本人，是同一个错误的另一面——受理真正的含义是「派给
 * 某人」，所以它就是派单。</p>
 */
public final class DormManagerRepairPage extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int SIDE_WIDTH = 440;

    private final BasePage page;
    private final DormClientService service;
    private final DormExtClientService ext;
    private final AsyncPagedTable<RepairOrderDto> repairs;
    private final JPanel side = new JPanel();
    private final List<RepairWorkerDto> workers = new ArrayList<RepairWorkerDto>();
    private final JComboBox<WorkerOption> workerSelect = new JComboBox<WorkerOption>();
    private RepairOrderDto selected;
    private RepairWorkOrderDto detail;

    public DormManagerRepairPage(BasePage page, DormClientService service, DormExtClientService ext) {
        super();
        setOpaque(false);
        setLayout(new BorderLayout());
        this.page = page;
        this.service = service;
        this.ext = ext;
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        repairs = table();
        renderSide();
        add(DormUi.split(left(), side, SIDE_WIDTH), BorderLayout.CENTER);
        loadWorkers();
    }

    public void reload() {
        repairs.reload();
        loadWorkers();
    }

    private JPanel left() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("报修工单", "按优先级和提交时间排序；选中一条在右侧派单。", null, false));
        column.add(repairs);
        return column;
    }

    private AsyncPagedTable<RepairOrderDto> table() {
        AsyncPagedTable<RepairOrderDto> table = new AsyncPagedTable<RepairOrderDto>("", "",
                "搜索类别、房间或描述",
                new String[]{"全部状态", "待派单", "已派单", "处理中", "待宿管审核", "已完成", "已取消"},
                new String[]{"工单", "房间", "报修人", "类别", "描述", "优先级", "状态", "维修员"},
                new AsyncPagedTable.Loader<RepairOrderDto>() {
                    @Override public PageSlice<RepairOrderDto> load(int p, String k, String f) throws Exception {
                        return RealUi.page(service.repairs(new DormPageQuery(p, 20, k, repairStatus(f), null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<RepairOrderDto>() {
                    @Override public Object[] values(RepairOrderDto row) {
                        return new Object[]{Long.valueOf(row.getId()), row.location(), reporter(row), RealUi.status(row.getCategory()),
                                RealUi.text(row.getDescription()), RealUi.status(row.getPriority()),
                                RealUi.status(row.getStatus()),
                                handler(row)};
                    }
                }, new AsyncPagedTable.SelectionListener<RepairOrderDto>() {
                    @Override public void onSelected(RepairOrderDto row) { select(row); }
                });
        return table;
    }

    // ---------- 右栏 ----------

    private void select(RepairOrderDto row) {
        selected = row;
        detail = null;
        renderSide();
        if (row == null) return;
        final long orderId = row.getId();
        AsyncTask.run(new AsyncTask.Work<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto run() throws Exception { return ext.repairDetail(orderId); }
        }, new AsyncTask.Callback<RepairWorkOrderDto>() {
            @Override public void onSuccess(RepairWorkOrderDto value) {
                if (selected != null && selected.getId() == orderId) { detail = value; renderSide(); }
            }
            @Override public void onFailure(Throwable error) { /* 派单不依赖它，静默降级 */ }
        });
    }

    private void loadWorkers() {
        AsyncTask.run(new AsyncTask.Work<List<RepairWorkerDto>>() {
            @Override public List<RepairWorkerDto> run() throws Exception { return ext.repairWorkers(); }
        }, new AsyncTask.Callback<List<RepairWorkerDto>>() {
            @Override public void onSuccess(List<RepairWorkerDto> value) {
                workers.clear();
                if (value != null) workers.addAll(value);
                renderSide();
            }
            @Override public void onFailure(Throwable error) { workers.clear(); renderSide(); }
        });
    }

    private void renderSide() {
        side.removeAll();
        if (selected == null) {
            side.add(DormUi.header("派单", "在左侧选中一条工单。", null, false));
            JLabel hint = DormUi.sub("选中后这里显示可派的维修员，以及学生是否授权不在场入内。");
            side.add(hint);
            DormUi.alignLeft(side);
            side.revalidate();
            side.repaint();
            return;
        }
        side.add(DormUi.header("派单 · 工单 " + selected.getId(),
                RealUi.status(selected.getCategory()) + "　·　" + selected.location()
                        + "　·　" + RealUi.dateTime(selected.getSubmittedAt()), null, false));

        if (detail != null) {
            side.add(DormUi.notice(detail.canEnterUnattended() ? DormUi.Tone.OK : DormUi.Tone.WARN,
                    detail.canEnterUnattended() ? "学生已授权不在场入内" : "学生未授权入内",
                    (detail.getContactPhone() == null || detail.getContactPhone().trim().isEmpty()
                            ? "学生没有登记手机号。" : "联系电话 " + detail.getContactPhone())
                            + (detail.getEntryNote() == null || detail.getEntryNote().trim().isEmpty()
                                    ? "" : "　备注：" + detail.getEntryNote().trim()),
                    null));
            side.add(Box.createVerticalStrut(16));
        }

        String status = selected.getStatus() == null ? ""
                : selected.getStatus().toUpperCase(java.util.Locale.ROOT);
        if (RepairWorkOrderDto.PENDING_REVIEW.equals(status)) {
            side.add(reviewBlock());
        } else if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            side.add(DormUi.sub("COMPLETED".equals(status)
                    ? "这单已经审核通过并结束，不再需要处理。" : "这单已经取消。"));
        } else {
            side.add(workerList());
        }
        side.add(Box.createVerticalStrut(18));
        side.add(cancelBlock(status));
        // 右栏是选中工单之后现搭的，这批组件没经过 DormUi.flatten，横向对齐还是 JPanel
        // 默认的居中值——BoxLayout 按各自的对齐值排，整栏就会看起来往右歪。
        DormUi.alignLeft(side);
        side.revalidate();
        side.repaint();
    }

    /** 维修员报完工之后由宿管拍板：认可就结束，不认可就退回去接着修。 */
    private JPanel reviewBlock() {
        JButton pass = new PrimaryButton("审核通过，结束工单");
        pass.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(true); }
        });
        JButton back = new SecondaryButton("打回重修");
        back.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { review(false); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(pass);
        buttons.add(back);

        JPanel box = DormUi.panel();
        box.add(buttons, BorderLayout.CENTER);
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("完工审核", "维修员已报完工，等你现场确认。", null, false));
        column.add(DormUi.notice(DormUi.Tone.WARN, "这单在等你审核",
                "通过则工单结束、学生可以评价；打回则退回处理中，仍由原来那位维修员继续。", null));
        column.add(Box.createVerticalStrut(12));
        column.add(box);
        return column;
    }

    private JPanel cancelBlock(String status) {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) return column;
        column.add(DormUi.header("取消工单", "误报或学生自行解决时使用；取消后不可恢复。", null, true));
        JButton cancel = new DangerButton("取消这张工单");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                update(DormRepairStatus.CANCELLED, true);
            }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(cancel);
        column.add(buttons);
        return column;
    }

    private void review(final boolean approved) {
        if (selected == null) return;
        final long orderId = selected.getId();
        AsyncTask.run(new AsyncTask.Work<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto run() throws Exception {
                return ext.reviewRepair(new RepairWorkRequest(orderId, approved ? null : "REJECT"));
            }
        }, new AsyncTask.Callback<RepairWorkOrderDto>() {
            @Override public void onSuccess(RepairWorkOrderDto value) {
                page.showSuccess(approved ? "工单 " + orderId + " 已审核通过并结束。"
                        : "工单 " + orderId + " 已打回，维修员那边会重新看到它。");
                repairs.reload();
                loadWorkers();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private JPanel workerList() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        JLabel caption = DormUi.caption("选择维修员");
        column.add(caption);
        column.add(Box.createVerticalStrut(8));
        workerSelect.removeAllItems();
        if (workers.isEmpty()) {
            JLabel empty = DormUi.sub("当前没有可派单的维修员。");
            column.add(empty);
            return column;
        }
        WorkerOption selectedOption = null;
        for (RepairWorkerDto worker : workers) {
            WorkerOption option = new WorkerOption(worker);
            workerSelect.addItem(option);
            if (detail != null && detail.getHandlerId() != null
                    && detail.getHandlerId().longValue() == worker.getUserId()) selectedOption = option;
        }
        if (selectedOption != null) workerSelect.setSelectedItem(selectedOption);
        else workerSelect.setSelectedIndex(0);
        column.add(workerSelect);
        column.add(Box.createVerticalStrut(12));
        JButton assign = new PrimaryButton(detail != null && detail.getHandlerId() != null
                ? "改派给选中的维修员" : "派给选中的维修员");
        assign.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { assign(); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(assign);
        column.add(buttons);
        return column;
    }

    private void assign() {
        if (selected == null) { page.showWarning("请先选择一条工单。"); return; }
        RepairWorkerDto worker = selectedWorker();
        if (worker == null) { page.showWarning("请先选择一个维修员。"); return; }
        final RepairAssignRequest request = new RepairAssignRequest(selected.getId(), worker.getUserId());
        final String name = worker.getDisplayName();
        AsyncTask.run(new AsyncTask.Work<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto run() throws Exception { return ext.assignRepair(request); }
        }, new AsyncTask.Callback<RepairWorkOrderDto>() {
            @Override public void onSuccess(RepairWorkOrderDto value) {
                page.showSuccess("工单 " + value.getOrderId() + " 已派给 " + name + "。");
                detail = value;
                repairs.reload();
                loadWorkers();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private RepairWorkerDto selectedWorker() {
        WorkerOption option = (WorkerOption) workerSelect.getSelectedItem();
        return option == null ? null : option.worker;
    }

    private static final class WorkerOption {
        private final RepairWorkerDto worker;
        private WorkerOption(RepairWorkerDto worker) { this.worker = worker; }
        @Override public String toString() { return worker.summary(); }
    }

    private void update(final DormRepairStatus target, boolean confirm) {
        if (selected == null) { page.showWarning("请先选择报修工单。"); return; }
        if (confirm && !RealUi.confirm(this, "确认取消该报修工单？")) return;
        final long orderId = selected.getId();
        AsyncTask.run(new AsyncTask.Work<RepairOrderDto>() {
            @Override public RepairOrderDto run() throws Exception {
                return service.updateRepair(new RepairStatusRequest(orderId, target.name()));
            }
        }, new AsyncTask.Callback<RepairOrderDto>() {
            @Override public void onSuccess(RepairOrderDto value) {
                page.showSuccess("报修状态已更新。");
                repairs.reload();
                loadWorkers();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static String repairStatus(String filter) {
        if ("待派单".equals(filter)) return DormRepairStatus.SUBMITTED.name();
        if ("已派单".equals(filter)) return DormRepairStatus.ACCEPTED.name();
        if ("处理中".equals(filter)) return DormRepairStatus.IN_PROGRESS.name();
        if ("待宿管审核".equals(filter)) return "PENDING_REVIEW";
        if ("已完成".equals(filter)) return DormRepairStatus.COMPLETED.name();
        if ("已取消".equals(filter)) return DormRepairStatus.CANCELLED.name();
        return null;
    }

    private static String reporter(RepairOrderDto row) {
        if (row.getReporterName() != null && !row.getReporterName().trim().isEmpty()) return row.getReporterName();
        if (row.getReporterUsername() != null && !row.getReporterUsername().trim().isEmpty()) return row.getReporterUsername();
        return "学生";
    }

    private static String handler(RepairOrderDto row) {
        if (row.getHandlerId() == null) return "未派单";
        if (row.getHandlerName() != null && !row.getHandlerName().trim().isEmpty()) return row.getHandlerName();
        if (row.getHandlerUsername() != null && !row.getHandlerUsername().trim().isEmpty()) return row.getHandlerUsername();
        return "维修员";
    }
}
