package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.WrapLayout;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.AiFeedbackEntry;
import edu.seu.vcampus.common.ai.AiFeedbackQuery;
import edu.seu.vcampus.common.ai.AiFeedbackTriageRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

/** 脱敏反馈的筛选、处理状态和知识关联闭环。 */
public final class AiFeedbackPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final LongConsumer openKnowledge;
    private final JComboBox<String> rating = new JComboBox<String>(new String[]{"全部评价", "HELPFUL", "UNHELPFUL"});
    private final JComboBox<String> process = new JComboBox<String>(new String[]{"全部状态", "PENDING", "RESOLVED", "IGNORED"});
    private final JTextField category = UiFactory.textField(10);
    private final JTextField from = UiFactory.textField(9);
    private final JTextField to = UiFactory.textField(9);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"评价", "问题类型", "问题摘要", "补充说明", "处理状态", "关联知识", "时间"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel summary = UiFactory.muted("—");
    private List<AiFeedbackEntry> rows = new ArrayList<AiFeedbackEntry>();

    public AiFeedbackPanel(AiAssistantClientService service) { this(service, null); }
    public AiFeedbackPanel(AiAssistantClientService service, LongConsumer openKnowledge) {
        super("用户反馈", "筛选并处理脱敏反馈，可关联到需要修订的知识片段。");
        this.service = service; this.openKnowledge = openKnowledge;
        setContent(content()); refresh();
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        JPanel filters = new JPanel(new WrapLayout(8)); filters.setOpaque(false);
        filters.add(UiFactory.body("评价")); filters.add(rating);
        filters.add(UiFactory.body("状态")); filters.add(process);
        filters.add(UiFactory.body("类型")); filters.add(category);
        from.setToolTipText("开始日期 yyyy-MM-dd"); to.setToolTipText("结束日期 yyyy-MM-dd");
        filters.add(UiFactory.body("日期")); filters.add(from); filters.add(UiFactory.muted("至")); filters.add(to);
        JButton query = new SecondaryButton("筛选"); query.addActionListener(e -> refresh()); filters.add(query);
        table.setAutoCreateRowSorter(true); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table); scroll.setPreferredSize(new Dimension(900, 420));
        JButton update = new PrimaryButton("更新处理状态"); update.addActionListener(e -> update());
        JButton details = new SecondaryButton("查看完整信息"); details.addActionListener(e -> details());
        JButton open = new SecondaryButton("打开关联知识"); open.addActionListener(e -> open());
        JButton refresh = new SecondaryButton("刷新"); refresh.addActionListener(e -> refresh());
        JPanel actions = new JPanel(new WrapLayout(8)); actions.setOpaque(false);
        actions.add(details); actions.add(update); actions.add(open); actions.add(refresh);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) details();
            }
        });
        JPanel south = new JPanel(new BorderLayout()); south.setOpaque(false);
        south.add(summary, BorderLayout.CENTER); south.add(actions, BorderLayout.EAST);
        root.add(filters, BorderLayout.NORTH); root.add(scroll, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH); return root;
    }

    private void refresh() {
        final AiFeedbackQuery query;
        try {
            query = new AiFeedbackQuery(choice(rating, "全部评价"), category.getText().trim(),
                    choice(process, "全部状态"), date(from.getText(), false), date(to.getText(), true));
        } catch (RuntimeException ex) { summary.setText("日期格式应为 yyyy-MM-dd"); return; }
        new SwingWorker<List<AiFeedbackEntry>, Void>() {
            private Exception failure;
            protected List<AiFeedbackEntry> doInBackground() {
                try { return service.feedback(query); } catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) { summary.setText("读取失败：" + failure.getMessage()); return; }
                    rows = get(); model.setRowCount(0); int helpful = 0, pending = 0;
                    for (AiFeedbackEntry row : rows) {
                        if ("HELPFUL".equals(row.getRating())) helpful++;
                        if ("PENDING".equals(row.getProcessStatus())) pending++;
                        model.addRow(new Object[]{"HELPFUL".equals(row.getRating()) ? "有帮助" : "需改进",
                                safe(row.getCategory()), safe(row.getQuestionPreview()), safe(row.getComment()),
                                statusText(row.getProcessStatus()), row.getRelatedChunkId() == null ? "" :
                                row.getRelatedChunkId() + " · " + safe(row.getRelatedChunkTitle()),
                                new java.util.Date(row.getCreatedAt())});
                    }
                    summary.setText("共 " + rows.size() + " 条 · 有帮助 " + helpful + " · 待处理 " + pending);
                } catch (Exception ex) { summary.setText("反馈响应读取失败"); }
            }
        }.execute();
    }

    private void update() {
        final AiFeedbackEntry selected = selected(); if (selected == null) return;
        JComboBox<String> status = new JComboBox<String>(new String[]{"PENDING", "RESOLVED", "IGNORED"});
        status.setSelectedItem(selected.getProcessStatus());
        JTextField chunk = UiFactory.textField(14);
        if (selected.getRelatedChunkId() != null) chunk.setText(selected.getRelatedChunkId().toString());
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(UiFactory.body("处理状态")); form.add(status);
        form.add(UiFactory.body("关联知识编号（可空）")); form.add(chunk);
        if (JOptionPane.showConfirmDialog(this, form, "处理反馈",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        final Long chunkId;
        try { chunkId = chunk.getText().trim().isEmpty() ? null : Long.valueOf(chunk.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "知识编号必须是数字。"); return; }
        final AiFeedbackTriageRequest request = new AiFeedbackTriageRequest(selected.getId(),
                (String) status.getSelectedItem(), chunkId);
        new SwingWorker<Boolean, Void>() {
            private Exception failure;
            protected Boolean doInBackground() { try { service.updateFeedback(request); return Boolean.TRUE; }
                catch (Exception ex) { failure = ex; return Boolean.FALSE; } }
            protected void done() { if (failure != null) JOptionPane.showMessageDialog(AiFeedbackPanel.this,
                    failure.getMessage()); else refresh(); }
        }.execute();
    }

    private void details() {
        AiFeedbackEntry selected = selected(); if (selected == null) return;
        JTextArea content = UiFactory.textArea(18, 62);
        content.setEditable(false); content.setText(detailText(selected)); content.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setPreferredSize(new Dimension(720, 430));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JOptionPane.showMessageDialog(this, scroll, "反馈完整信息",
                JOptionPane.PLAIN_MESSAGE);
    }

    static String detailText(AiFeedbackEntry entry) {
        String related = entry.getRelatedChunkId() == null ? "未关联"
                : entry.getRelatedChunkId() + " · " + safeValue(entry.getRelatedChunkTitle());
        return "反馈编号：" + entry.getId()
                + "\n评价：" + ("HELPFUL".equals(entry.getRating()) ? "有帮助" : "需改进")
                + "\n问题类型：" + display(entry.getCategory())
                + "\n处理状态：" + displayStatus(entry.getProcessStatus())
                + "\n关联知识：" + related
                + "\n提交时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(entry.getCreatedAt()))
                + "\n\n问题摘要：\n" + display(entry.getQuestionPreview())
                + "\n\n补充说明：\n" + display(entry.getComment());
    }

    private void open() {
        AiFeedbackEntry selected = selected();
        if (selected == null || selected.getRelatedChunkId() == null) {
            JOptionPane.showMessageDialog(this, "该反馈尚未关联知识片段。"); return;
        }
        if (openKnowledge != null) openKnowledge.accept(selected.getRelatedChunkId().longValue());
    }
    private AiFeedbackEntry selected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择一条反馈。"); return null; }
        return rows.get(table.convertRowIndexToModel(row));
    }
    private String choice(JComboBox<String> box, String all) {
        String value = (String) box.getSelectedItem(); return all.equals(value) ? null : value;
    }
    private Long date(String value, boolean exclusiveEnd) {
        String clean = value == null ? "" : value.trim(); if (clean.isEmpty()) return null;
        LocalDate day = LocalDate.parse(clean); if (exclusiveEnd) day = day.plusDays(1);
        return Long.valueOf(day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
    private String statusText(String value) {
        return displayStatus(value);
    }
    private String safe(String value) { return value == null ? "" : value; }
    private static String display(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }
    private static String safeValue(String value) { return value == null ? "" : value; }
    private static String displayStatus(String value) {
        if ("RESOLVED".equals(value)) return "已处理";
        if ("IGNORED".equals(value)) return "已忽略"; return "待处理";
    }
}
