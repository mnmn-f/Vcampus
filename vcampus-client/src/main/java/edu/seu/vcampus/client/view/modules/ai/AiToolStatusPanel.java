package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.AiToolStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** AI 工具目录与实际业务命令接入状态。 */
public final class AiToolStatusPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"状态", "类型", "工具", "能力", "参数与澄清要求"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel summary = UiFactory.muted("—");

    public AiToolStatusPanel(AiAssistantClientService service) {
        super("校园工具状态", "核对代办能力是否已连接对应业务模块，并查看结构化参数要求。");
        this.service = service; setContent(content()); refresh();
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        JTable table = new JTable(model); table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(4).setPreferredWidth(330);
        JScrollPane scroll = new JScrollPane(table); scroll.setPreferredSize(new Dimension(830, 460));
        JButton refresh = new SecondaryButton("重新检测"); refresh.addActionListener(e -> refresh());
        JPanel south = new JPanel(new BorderLayout()); south.setOpaque(false);
        south.add(summary, BorderLayout.CENTER); south.add(refresh, BorderLayout.EAST);
        root.add(scroll, BorderLayout.CENTER); root.add(south, BorderLayout.SOUTH); return root;
    }

    private void refresh() {
        new SwingWorker<List<AiToolStatus>, Void>() {
            private Exception failure;
            protected List<AiToolStatus> doInBackground() {
                try { return service.toolStatuses(); } catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) { summary.setText("检测失败：" + failure.getMessage()); return; }
                    model.setRowCount(0); int ready = 0;
                    for (AiToolStatus row : get()) {
                        if (row.isAvailable()) ready++;
                        model.addRow(new Object[]{row.isAvailable() ? "可用" : "未接入",
                                row.isWriteOperation() ? "写操作" : "查询",
                                row.getName(), row.getDescription(), row.getParameterGuide()});
                    }
                    summary.setText("已接入 " + ready + " / " + model.getRowCount()
                            + " · 写操作仍需学生确认");
                } catch (Exception ex) { summary.setText("工具状态响应读取失败"); }
            }
        }.execute();
    }
}
