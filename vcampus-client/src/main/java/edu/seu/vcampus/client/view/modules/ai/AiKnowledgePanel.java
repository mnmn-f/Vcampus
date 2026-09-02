package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/** AI 知识管理员的检索、编辑、停用和文本导入界面。 */
public final class AiKnowledgePanel extends SectionCard {
    private final AiAssistantClientService service;
    private final JTextField keyword = UiFactory.textField(18);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"编号", "来源", "标题", "状态", "更新时间"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField source = UiFactory.textField(14);
    private final JTextField title = UiFactory.textField(24);
    private final JTextArea content = UiFactory.textArea(7, 60);
    private final JComboBox<String> status = new JComboBox<String>(
            new String[]{"ACTIVE", "INACTIVE"});
    private List<AiKnowledgeChunk> rows;
    private Long editingId;

    public AiKnowledgePanel(AiAssistantClientService service) {
        super("知识库管理", "维护可追溯知识片段；停用不会破坏历史会话与审计记录。");
        this.service = service; setContent(content()); bind(); load();
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 12)); root.setOpaque(false);
        JPanel filters = UiFactory.horizontal(8); filters.add(UiFactory.body("关键词"));
        filters.add(keyword); JButton search = new SecondaryButton("查询");
        search.setActionCommand("search"); filters.add(search);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane grid = new JScrollPane(table); grid.setPreferredSize(new Dimension(760, 220));
        JPanel editor = new JPanel(new GridBagLayout()); editor.setOpaque(false);
        GridBagConstraints a = UiFactory.gbc(0, 0);
        editor.add(UiFactory.labelledField("来源类型", source), a);
        GridBagConstraints b = UiFactory.gbc(1, 0); b.weightx = 1;
        b.fill = GridBagConstraints.HORIZONTAL;
        editor.add(UiFactory.labelledField("标题", title), b);
        GridBagConstraints c = UiFactory.gbc(2, 0);
        editor.add(UiFactory.labelledField("状态", status), c);
        GridBagConstraints d = UiFactory.gbc(0, 1); d.gridwidth = 3;
        d.weightx = 1; d.fill = GridBagConstraints.BOTH;
        editor.add(UiFactory.labelledField("知识正文", new JScrollPane(content)), d);
        JPanel actions = UiFactory.horizontal(8);
        JButton fresh = new SecondaryButton("新建");
        JButton importText = new SecondaryButton("导入文本文件");
        JButton save = new PrimaryButton("保存并更新索引");
        JButton delete = new DangerButton("停用选中片段");
        fresh.setActionCommand("fresh"); importText.setActionCommand("import");
        save.setActionCommand("save"); delete.setActionCommand("delete");
        actions.add(fresh); actions.add(importText); actions.add(save); actions.add(delete);
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) { action(e.getActionCommand()); }
        };
        search.addActionListener(listener); fresh.addActionListener(listener);
        importText.addActionListener(listener); save.addActionListener(listener);
        delete.addActionListener(listener);
        JPanel south = new JPanel(new BorderLayout()); south.setOpaque(false);
        south.add(editor, BorderLayout.CENTER); south.add(actions, BorderLayout.SOUTH);
        root.add(filters, BorderLayout.NORTH); root.add(grid, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH); return root;
    }

    private void bind() {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) select();
            }
        });
    }

    private void action(String value) {
        if ("search".equals(value)) load();
        else if ("fresh".equals(value)) clear();
        else if ("import".equals(value)) importText();
        else if ("save".equals(value)) save();
        else if ("delete".equals(value)) delete();
    }

    private void load() {
        task(new Work<AiPage<AiKnowledgeChunk>>() {
            public AiPage<AiKnowledgeChunk> run() throws Exception {
                return service.knowledge(new AiKnowledgeQuery(keyword.getText(), null, 1, 100));
            }
        }, new Result<AiPage<AiKnowledgeChunk>>() {
            public void accept(AiPage<AiKnowledgeChunk> page) {
                rows = page.getItems(); model.setRowCount(0);
                for (AiKnowledgeChunk row : rows) model.addRow(new Object[]{row.getChunkId(),
                        row.getSourceType(), row.getTitle(), row.getStatus(),
                        new java.util.Date(row.getUpdatedAt())});
            }
        });
    }

    private void select() {
        int index = table.getSelectedRow(); if (index < 0 || rows == null) return;
        AiKnowledgeChunk row = rows.get(table.convertRowIndexToModel(index));
        editingId = Long.valueOf(row.getChunkId()); source.setText(row.getSourceType());
        title.setText(row.getTitle()); content.setText(row.getContent());
        status.setSelectedItem(row.getStatus());
    }

    private void clear() {
        editingId = null; table.clearSelection(); source.setText("MANUAL");
        title.setText(""); content.setText(""); status.setSelectedItem("ACTIVE");
    }

    private void save() {
        final AiKnowledgeSaveRequest request = new AiKnowledgeSaveRequest(editingId,
                source.getText().trim(), title.getText().trim(), content.getText().trim(),
                (String) status.getSelectedItem());
        task(new Work<AiKnowledgeChunk>() {
            public AiKnowledgeChunk run() throws Exception { return service.saveKnowledge(request); }
        }, new Result<AiKnowledgeChunk>() {
            public void accept(AiKnowledgeChunk value) {
                editingId = Long.valueOf(value.getChunkId()); load();
            }
        });
    }

    private void delete() {
        if (editingId == null) return;
        if (JOptionPane.showConfirmDialog(this, "停用后该片段不再参与问答检索，是否继续？",
                "停用知识", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        final long id = editingId.longValue();
        task(new Work<Boolean>() {
            public Boolean run() throws Exception { service.deleteKnowledge(id); return Boolean.TRUE; }
        }, new Result<Boolean>() {
            public void accept(Boolean ignored) { clear(); load(); }
        });
    }

    private void importText() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File file = chooser.getSelectedFile();
        if (file.length() > 1024L * 1024L) {
            JOptionPane.showMessageDialog(this, "文本文件不能超过 1 MiB。"); return;
        }
        try {
            java.io.InputStream in = new java.io.FileInputStream(file);
            try {
                byte[] data = new byte[(int) file.length()]; int offset = 0, read;
                while (offset < data.length
                        && (read = in.read(data, offset, data.length - offset)) >= 0) offset += read;
                content.setText(new String(data, 0, offset, "UTF-8"));
                title.setText(file.getName()); source.setText("FILE");
            } finally { in.close(); }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "文件读取失败：" + ex.getMessage());
        }
    }

    private <T> void task(final Work<T> work, final Result<T> result) {
        new SwingWorker<T, Void>() {
            private Exception failure;
            protected T doInBackground() {
                try { return work.run(); } catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) JOptionPane.showMessageDialog(
                            AiKnowledgePanel.this, failure.getMessage());
                    else result.accept(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AiKnowledgePanel.this, "知识服务响应失败。");
                }
            }
        }.execute();
    }
    private interface Work<T> { T run() throws Exception; }
    private interface Result<T> { void accept(T value); }
}
