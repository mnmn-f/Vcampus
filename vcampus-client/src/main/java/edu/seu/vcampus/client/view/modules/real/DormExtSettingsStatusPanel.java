package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.ext.DormExtStatusDto;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

/** 服务端调度器状态和手动任务入口。 */
final class DormExtSettingsStatusPanel extends JPanel {
    private static final String[] TASKS = {"全部", "absenceScan", "warningNotify",
            "hygieneTaskGenerate", "noticeExpireScan", "billGenerate"};
    private final BasePage page;
    private final DormExtClientService service;
    private final JLabel state = UiFactory.body("—");
    private final JComboBox<String> task = new JComboBox<String>(TASKS);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"任务", "周期", "最近一次", "累计"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    DormExtSettingsStatusPanel(BasePage page, DormExtClientService service) {
        this.page = page;
        this.service = service;
        task.setFont(DesignTokens.regular(13));
        setOpaque(false);
        setLayout(new BorderLayout());
        add(card(), BorderLayout.CENTER);
        reload();
    }

    void reload() { loadStatus(); }

    private JPanel card() {
        SectionCard card = new SectionCard("服务端运行状态", "查看定时任务执行记录，或手动执行一次任务。");
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(DesignTokens.regular(13));
        table.setEnabled(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(0, 160));
        JPanel top = UiFactory.horizontal(8);
        top.add(UiFactory.body("调度器")); top.add(state);
        JPanel line = UiFactory.horizontal(8);
        line.add(UiFactory.body("任务")); line.add(task);
        line.add(DormExtSettingsSupport.button("立即执行", true, this::runTask));
        line.add(DormExtSettingsSupport.button("刷新状态", false, this::loadStatus));
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(top, BorderLayout.NORTH); content.add(scroll, BorderLayout.CENTER);
        content.add(line, BorderLayout.SOUTH); card.setContent(content);
        JPanel result = new JPanel(new BorderLayout()); result.setOpaque(false);
        result.add(card, BorderLayout.CENTER); return result;
    }

    private void loadStatus() {
        AsyncTask.run(() -> service.status(), new AsyncTask.Callback<DormExtStatusDto>() {
            @Override public void onSuccess(DormExtStatusDto value) { show(value); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void runTask() {
        final String selected = String.valueOf(task.getSelectedItem());
        final String name = "全部".equals(selected) ? null : selected;
        AsyncTask.run(() -> service.runScheduledTask(name), new AsyncTask.Callback<DormExtStatusDto>() {
            @Override public void onSuccess(DormExtStatusDto value) {
                show(value); page.showSuccess("已执行 " + selected + "，结果见上表。");
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void show(DormExtStatusDto value) {
        state.setText(value.isSchedulerRunning() ? "运行中 · " + RealUi.text(value.getModuleVersion())
                + " · 服务端时间 " + RealUi.dateTime(value.getServerTime())
                : "未启动 · " + RealUi.text(value.getModuleVersion()));
        model.setRowCount(0);
        List<String> lines = value.getScheduledTasks();
        if (lines.isEmpty()) { model.addRow(new Object[]{"（调度器未启动）", "", "", ""}); return; }
        for (String line : lines) {
            String[] parts = line.split(" \\| ", 4);
            Object[] row = new Object[]{"", "", "", ""};
            System.arraycopy(parts, 0, row, 0, Math.min(parts.length, 4)); model.addRow(row);
        }
    }
}
