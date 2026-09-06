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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;

/** AI 知识管理员的检索、编辑、停用和文本导入界面。 */
public final class AiKnowledgePanel extends SectionCard {
    private final AiAssistantClientService service;
    private final JTextField keyword = UiFactory.textField(18);
    private final JComboBox<String> statusFilter = new JComboBox<String>(
            new String[]{"全部状态", "ACTIVE", "INACTIVE"});
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
    private final JLabel characterCount = UiFactory.muted("0 字");
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
        search.setActionCommand("search"); filters.add(statusFilter); filters.add(search);
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
        JButton importText = new SecondaryButton("导入文档并分段");
        JButton versions = new SecondaryButton("版本历史");
        JButton save = new PrimaryButton("保存并更新索引");
        JButton delete = new DangerButton("停用选中片段");
        fresh.setActionCommand("fresh"); importText.setActionCommand("import");
        versions.setActionCommand("versions"); save.setActionCommand("save");
        delete.setActionCommand("delete");
        actions.add(characterCount);
        actions.add(fresh); actions.add(importText); actions.add(save); actions.add(delete);
        actions.add(versions);
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) { action(e.getActionCommand()); }
        };
        search.addActionListener(listener); fresh.addActionListener(listener);
        importText.addActionListener(listener); save.addActionListener(listener);
        delete.addActionListener(listener);
        versions.addActionListener(listener);
        JPanel south = new JPanel(new BorderLayout()); south.setOpaque(false);
        south.add(editor, BorderLayout.CENTER); south.add(actions, BorderLayout.SOUTH);
        root.add(filters, BorderLayout.NORTH); root.add(grid, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH); return root;
    }

    private void bind() {
        content.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { count(); }
            public void removeUpdate(DocumentEvent e) { count(); }
            public void changedUpdate(DocumentEvent e) { count(); }
        });
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
        else if ("versions".equals(value)) versions();
    }

    private void load() {
        task(new Work<AiPage<AiKnowledgeChunk>>() {
            public AiPage<AiKnowledgeChunk> run() throws Exception {
                String selected = (String) statusFilter.getSelectedItem();
                return service.knowledge(new AiKnowledgeQuery(keyword.getText(),
                        "全部状态".equals(selected) ? null : selected, 1, 100));
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
        final java.io.File file = chooser.getSelectedFile();
        try {
            AiAttachment attachment = new AiAttachmentLoader().load(file);
            if (!attachment.isText()) {
                JOptionPane.showMessageDialog(this, "知识库导入需要包含可提取文字的文档。"); return;
            }
            final List<String> chunks = split(new String(attachment.getContent(), StandardCharsets.UTF_8));
            int decision = JOptionPane.showConfirmDialog(this,
                    "将“" + file.getName() + "”导入为 " + chunks.size() + " 个知识片段，是否继续？",
                    "批量导入知识", JOptionPane.YES_NO_OPTION);
            if (decision != JOptionPane.YES_OPTION) return;
            task(new Work<Integer>() {
                public Integer run() throws Exception {
                    for (int index = 0; index < chunks.size(); index++) {
                        service.saveKnowledge(new AiKnowledgeSaveRequest(null, "FILE",
                                file.getName() + (chunks.size() == 1 ? "" : "（" + (index + 1)
                                        + "/" + chunks.size() + "）"), chunks.get(index), "ACTIVE"));
                    }
                    return Integer.valueOf(chunks.size());
                }
            }, new Result<Integer>() {
                public void accept(Integer imported) {
                    JOptionPane.showMessageDialog(AiKnowledgePanel.this,
                            "已导入 " + imported + " 个知识片段。"); load();
                }
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "文件读取失败：" + ex.getMessage());
        }
    }

    private List<String> split(String value) {
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : normalized.split("\\n\\s*\\n")) {
            if (paragraph.length() > 3500) {
                if (current.length() > 0) { result.add(current.toString()); current.setLength(0); }
                for (int start = 0; start < paragraph.length(); start += 3500) {
                    result.add(paragraph.substring(start, Math.min(paragraph.length(), start + 3500)));
                }
            } else if (current.length() > 0 && current.length() + paragraph.length() + 2 > 3500) {
                result.add(current.toString()); current.setLength(0); current.append(paragraph);
            } else {
                if (current.length() > 0) current.append("\n\n"); current.append(paragraph);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        if (result.isEmpty()) result.add(normalized);
        return result;
    }

    private void versions() {
        if (editingId == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个知识片段。"); return;
        }
        final long chunkId = editingId.longValue();
        task(new Work<List<AiKnowledgeVersion>>() {
            public List<AiKnowledgeVersion> run() throws Exception {
                return service.knowledgeVersions(chunkId);
            }
        }, new Result<List<AiKnowledgeVersion>>() {
            public void accept(final List<AiKnowledgeVersion> history) {
                if (history.isEmpty()) { JOptionPane.showMessageDialog(AiKnowledgePanel.this,
                        "该片段还没有版本记录。"); return; }
                final JComboBox<AiKnowledgeVersion> choices = new JComboBox<AiKnowledgeVersion>(
                        history.toArray(new AiKnowledgeVersion[history.size()]));
                final JTextArea preview = UiFactory.textArea(12, 56); preview.setEditable(false);
                Runnable show = () -> preview.setText(((AiKnowledgeVersion) choices.getSelectedItem()).getContent());
                choices.addActionListener(e -> show.run()); show.run();
                JPanel panel = new JPanel(new BorderLayout(0, 8));
                panel.add(choices, BorderLayout.NORTH); panel.add(new JScrollPane(preview), BorderLayout.CENTER);
                if (JOptionPane.showConfirmDialog(AiKnowledgePanel.this, panel,
                        "选择要恢复的历史版本", JOptionPane.OK_CANCEL_OPTION)
                        != JOptionPane.OK_OPTION) return;
                final AiKnowledgeVersion selected = (AiKnowledgeVersion) choices.getSelectedItem();
                task(new Work<AiKnowledgeChunk>() {
                    public AiKnowledgeChunk run() throws Exception {
                        return service.rollbackKnowledge(chunkId, selected.getVersionId());
                    }
                }, new Result<AiKnowledgeChunk>() {
                    public void accept(AiKnowledgeChunk restored) {
                        source.setText(restored.getSourceType()); title.setText(restored.getTitle());
                        content.setText(restored.getContent()); status.setSelectedItem(restored.getStatus()); load();
                    }
                });
            }
        });
    }

    private void count() { characterCount.setText(content.getText().length() + " 字"); }

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
