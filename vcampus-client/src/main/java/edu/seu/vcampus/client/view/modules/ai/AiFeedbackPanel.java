package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.AiFeedbackEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** 脱敏后的回答质量反馈，不展示身份与完整会话。 */
public final class AiFeedbackPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"评价", "问题类型", "问题摘要", "补充说明", "时间"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel summary = UiFactory.muted("—");

    public AiFeedbackPanel(AiAssistantClientService service) {
        super("用户反馈", "仅显示脱敏问题摘要，用于发现知识缺口和工具失败模式。");
        this.service = service; setContent(content()); refresh();
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        JTable table = new JTable(model); table.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(table); scroll.setPreferredSize(new Dimension(790, 430));
        JButton refresh = new SecondaryButton("刷新反馈"); refresh.addActionListener(e -> refresh());
        JPanel south = new JPanel(new BorderLayout()); south.setOpaque(false);
        south.add(summary, BorderLayout.CENTER); south.add(refresh, BorderLayout.EAST);
        root.add(scroll, BorderLayout.CENTER); root.add(south, BorderLayout.SOUTH); return root;
    }

    private void refresh() {
        new SwingWorker<List<AiFeedbackEntry>, Void>() {
            private Exception failure;
            protected List<AiFeedbackEntry> doInBackground() {
                try { return service.feedback(); } catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) { summary.setText("读取失败：" + failure.getMessage()); return; }
                    model.setRowCount(0); int helpful = 0;
                    for (AiFeedbackEntry row : get()) {
                        if ("HELPFUL".equals(row.getRating())) helpful++;
                        model.addRow(new Object[]{"HELPFUL".equals(row.getRating()) ? "有帮助" : "需改进",
                                safe(row.getCategory()), safe(row.getQuestionPreview()),
                                safe(row.getComment()), new java.util.Date(row.getCreatedAt())});
                    }
                    int total = model.getRowCount();
                    summary.setText("最近 " + total + " 条 · 有帮助 " + helpful
                            + " · 需改进 " + (total - helpful));
                } catch (Exception ex) { summary.setText("反馈响应读取失败"); }
            }
        }.execute();
    }

    private String safe(String value) { return value == null ? "" : value; }
}
