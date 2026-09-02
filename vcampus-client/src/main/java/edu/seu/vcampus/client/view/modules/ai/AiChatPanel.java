package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/** 学生端会话、流式回答、取消和写操作确认界面。 */
public final class AiChatPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final JComboBox<AiSessionSummary> sessions = new JComboBox<AiSessionSummary>();
    private final JTextArea transcript = UiFactory.textArea(18, 72);
    private final JTextArea input = UiFactory.textArea(3, 60);
    private final JButton send = new PrimaryButton("发送");
    private final JButton cancel = new SecondaryButton("停止");
    private String requestId;
    private boolean loading;

    public AiChatPanel(AiAssistantClientService service) {
        super("校园助手对话", "写操作只生成待确认记录；确认后仍由原业务模块再次鉴权。");
        this.service = service; setContent(content()); bind(); loadSessions(null);
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 12)); root.setOpaque(false);
        JPanel tools = UiFactory.horizontal(8);
        sessions.setPreferredSize(new Dimension(300, 36));
        JButton create = new SecondaryButton("新建会话");
        JButton clear = new SecondaryButton("清空当前会话");
        JButton refresh = new SecondaryButton("刷新");
        create.setActionCommand("create"); clear.setActionCommand("clear");
        refresh.setActionCommand("refresh");
        ActionListener actions = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ("create".equals(e.getActionCommand())) createSession();
                else if ("clear".equals(e.getActionCommand())) clearSession();
                else loadSessions(selectedId());
            }
        };
        create.addActionListener(actions); clear.addActionListener(actions);
        refresh.addActionListener(actions);
        tools.add(UiFactory.body("会话")); tools.add(sessions); tools.add(create);
        tools.add(clear); tools.add(refresh);
        transcript.setEditable(false); transcript.setBackground(new Color(0xFA, 0xFB, 0xF8));
        JScrollPane history = new JScrollPane(transcript); history.setBorder(null);
        JPanel composer = new JPanel(new BorderLayout(8, 0)); composer.setOpaque(false);
        composer.add(new JScrollPane(input), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 8)); buttons.setOpaque(false);
        buttons.add(send); buttons.add(cancel); composer.add(buttons, BorderLayout.EAST);
        cancel.setEnabled(false);
        root.add(tools, BorderLayout.NORTH); root.add(history, BorderLayout.CENTER);
        root.add(composer, BorderLayout.SOUTH); return root;
    }

    private void bind() {
        sessions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { if (!loading) loadHistory(selectedId()); }
        });
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { send(); }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (requestId != null) service.cancel(requestId);
                finish("回答已停止。", false);
            }
        });
    }

    private void send() {
        final String text = input.getText().trim();
        if (text.isEmpty() || requestId != null) return;
        append("我", text); input.setText(""); send.setEnabled(false); cancel.setEnabled(true);
        transcript.append("\n\n校园助手：");
        requestId = service.query(selectedId(), text, new AiConversationListener() {
            public void onChunk(final String value) {
                ui(new Runnable() { public void run() { appendChunk(value); }});
            }
            public void onComplete() {
                ui(new Runnable() { public void run() { finish(null, true); }});
            }
            public void onFailure(final String value) {
                ui(new Runnable() { public void run() { finish(value, false); }});
            }
            public void onActionRequired(final AiPendingAction action) {
                ui(new Runnable() { public void run() { confirm(action); }});
            }
        });
    }

    private void confirm(final AiPendingAction action) {
        int choice = JOptionPane.showConfirmDialog(this, action.getSummary()
                + "\n\n确认后将由原业务模块执行，5 分钟内有效。", "确认业务操作",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        final boolean agreed = choice == JOptionPane.YES_OPTION;
        new SwingWorker<AiConfirmResult, Void>() {
            private Exception failure;
            protected AiConfirmResult doInBackground() {
                try { return service.confirm(action.getActionId(), agreed); }
                catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) append("系统", failure.getMessage());
                    else append("校园助手", get().getContent());
                } catch (Exception ex) { append("系统", "确认结果读取失败。"); }
            }
        }.execute();
    }

    private void finish(String message, boolean reload) {
        if (message != null) append("系统", message);
        requestId = null; send.setEnabled(true); cancel.setEnabled(false);
        if (reload) loadSessions(selectedId());
    }

    private void createSession() {
        task(new Work<String>() { public String run() throws Exception {
            return service.createSession();
        }}, new Result<String>() { public void accept(String id) { loadSessions(id); }});
    }

    private void clearSession() {
        final String id = selectedId(); if (id == null) return;
        if (JOptionPane.showConfirmDialog(this, "清空后会话历史不可恢复，是否继续？",
                "清空会话", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        task(new Work<Boolean>() { public Boolean run() throws Exception {
            service.clearSession(id); return Boolean.TRUE;
        }}, new Result<Boolean>() { public void accept(Boolean ignored) {
            transcript.setText(""); loadSessions(null);
        }});
    }

    private void loadSessions(final String preferred) {
        task(new Work<List<AiSessionSummary>>() { public List<AiSessionSummary> run()
                throws Exception { return service.sessions(); }},
                new Result<List<AiSessionSummary>>() {
            public void accept(List<AiSessionSummary> values) {
                loading = true; sessions.removeAllItems(); AiSessionSummary selected = null;
                for (AiSessionSummary value : values) {
                    sessions.addItem(value);
                    if (preferred != null && preferred.equals(value.getSessionId())) selected = value;
                }
                if (selected != null) sessions.setSelectedItem(selected);
                loading = false; loadHistory(selectedId());
            }
        });
    }

    private void loadHistory(final String id) {
        if (id == null) { transcript.setText(""); return; }
        task(new Work<List<AiChatMessage>>() { public List<AiChatMessage> run()
                throws Exception { return service.history(id); }},
                new Result<List<AiChatMessage>>() {
            public void accept(List<AiChatMessage> values) {
                transcript.setText("");
                for (AiChatMessage m : values) append(
                        "USER".equals(m.getSenderType()) ? "我" : "校园助手", m.getContent());
            }
        });
    }

    private String selectedId() {
        Object value = sessions.getSelectedItem();
        return value instanceof AiSessionSummary ? ((AiSessionSummary) value).getSessionId() : null;
    }
    private void append(String role, String text) {
        transcript.append((transcript.getText().isEmpty() ? "" : "\n\n") + role + "：" + text);
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }
    private void appendChunk(String text) {
        transcript.append(text); transcript.setCaretPosition(transcript.getDocument().getLength());
    }
    private void ui(Runnable work) { SwingUtilities.invokeLater(work); }

    private <T> void task(final Work<T> work, final Result<T> result) {
        new SwingWorker<T, Void>() {
            private Exception failure;
            protected T doInBackground() {
                try { return work.run(); } catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) append("系统", failure.getMessage());
                    else result.accept(get());
                } catch (Exception ex) { append("系统", "AI 服务响应失败。"); }
            }
        }.execute();
    }
    private interface Work<T> { T run() throws Exception; }
    private interface Result<T> { void accept(T value); }
}
