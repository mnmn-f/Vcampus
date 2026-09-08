package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.WrapLayout;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.AiKnowledgeTestResult;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 一次运行多条知识问答用例，检查知识更新带来的回归影响。 */
public final class AiKnowledgeRegressionPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"运行", "问题", "标准答案关键词（| 分隔）", "预期命中知识", "结果"}, 0) {
        public Class<?> getColumnClass(int column) { return column == 0 ? Boolean.class : Object.class; }
        public boolean isCellEditable(int row, int column) { return column < 4; }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = UiFactory.muted("添加用例后可批量回归。");

    public AiKnowledgeRegressionPanel(AiAssistantClientService service) {
        super("批量回归测试", "知识更新后批量核对标准答案关键词和预期命中知识。");
        this.service = service; setContent(content());
        model.addRow(new Object[]{Boolean.TRUE, "图书馆开放时间是什么？", "", "", "未运行"});
    }
    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        table.setRowHeight(30); table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.getColumnModel().getColumn(4).setPreferredWidth(220);
        JScrollPane scroll = new JScrollPane(table); scroll.setPreferredSize(new Dimension(900, 360));
        JButton add = new SecondaryButton("添加用例"); add.addActionListener(e -> model.addRow(
                new Object[]{Boolean.TRUE, "", "", "", "未运行"}));
        JButton remove = new SecondaryButton("删除选中"); remove.addActionListener(e -> {
            int row = table.getSelectedRow(); if (row >= 0) model.removeRow(table.convertRowIndexToModel(row));
        });
        JButton run = new PrimaryButton("运行选中用例"); run.addActionListener(e -> runAll());
        JPanel actions = new JPanel(new WrapLayout(8)); actions.setOpaque(false);
        actions.add(add); actions.add(remove); actions.add(run);
        JPanel south = new JPanel(new BorderLayout()); south.setOpaque(false);
        south.add(status, BorderLayout.CENTER); south.add(actions, BorderLayout.EAST);
        root.add(scroll, BorderLayout.CENTER); root.add(south, BorderLayout.SOUTH); return root;
    }
    private void runAll() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        final List<Case> cases = new ArrayList<Case>();
        for (int row = 0; row < model.getRowCount(); row++) if (Boolean.TRUE.equals(model.getValueAt(row, 0))) {
            String question = text(row, 1); if (!question.isEmpty()) cases.add(new Case(row, question,
                    text(row, 2), text(row, 3)));
        }
        if (cases.isEmpty()) { status.setText("没有可运行的用例。"); return; }
        status.setText("正在运行 " + cases.size() + " 条用例……");
        new SwingWorker<List<Outcome>, Void>() {
            private Exception failure;
            protected List<Outcome> doInBackground() {
                List<Outcome> out = new ArrayList<Outcome>();
                try {
                    for (Case item : cases) {
                        AiKnowledgeTestResult value = service.testKnowledge(item.question);
                        boolean answer = AiKnowledgeTestPanel.matchesAnswer(value.getAnswer(), item.answer);
                        boolean hit = AiKnowledgeTestPanel.matchesHit(value, item.hit);
                        out.add(new Outcome(item.row, answer && hit, answer, hit,
                                value.getMatches().size()));
                    }
                } catch (Exception ex) { failure = ex; }
                return out;
            }
            protected void done() {
                try {
                    List<Outcome> values = get(); int passed = 0;
                    for (Outcome value : values) {
                        if (value.passed) passed++;
                        model.setValueAt(value.passed ? "通过 · 命中 " + value.hits :
                                "失败" + (!value.answer ? " · 答案" : "")
                                        + (!value.hit ? " · 知识" : "") + " · 命中 " + value.hits,
                                value.row, 4);
                    }
                    status.setText("已完成 " + values.size() + " 条 · 通过 " + passed + " · 失败 "
                            + (values.size() - passed) + (failure == null ? "" : " · 中止：" + failure.getMessage()));
                } catch (Exception ex) { status.setText("回归测试响应读取失败"); }
            }
        }.execute();
    }
    private String text(int row, int column) {
        Object value = model.getValueAt(row, column); return value == null ? "" : value.toString().trim();
    }
    private static final class Case {
        private final int row; private final String question, answer, hit;
        private Case(int row, String question, String answer, String hit) {
            this.row = row; this.question = question; this.answer = answer; this.hit = hit;
        }
    }
    private static final class Outcome {
        private final int row, hits; private final boolean passed, answer, hit;
        private Outcome(int row, boolean passed, boolean answer, boolean hit, int hits) {
            this.row = row; this.passed = passed; this.answer = answer; this.hit = hit; this.hits = hits;
        }
    }
}
