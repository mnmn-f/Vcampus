package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.pet.PetActivityListener;
import edu.seu.vcampus.client.view.pet.PetMood;
import edu.seu.vcampus.common.ai.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** 学生端会话、流式回答、取消和写操作确认界面。 */
public final class AiChatPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final PetActivityListener petActivity;
    private final DefaultListModel<AiSessionSummary> sessionModel =
            new DefaultListModel<AiSessionSummary>();
    private final JList<AiSessionSummary> sessions = new JList<AiSessionSummary>(sessionModel);
    private final JPanel messageList = new JPanel();
    private final StringBuilder transcriptText = new StringBuilder();
    private final JTextArea input = UiFactory.textArea(3, 60);
    private final JComboBox<AiMode> modes = new JComboBox<AiMode>(AiMode.values());
    private final JButton send = new PrimaryButton("发送");
    private final JButton cancel = new SecondaryButton("停止");
    private final JButton retry = new SecondaryButton("重试");
    private final JButton attach = new SecondaryButton("+");
    private final JPanel attachmentChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
    private final JTextField sessionSearch = UiFactory.textField(10);
    private final JButton helpful = new SecondaryButton("👍 点赞");
    private final JButton unhelpful = new SecondaryButton("👎 点踩");
    private final JButton correct = new SecondaryButton("纠错");
    private final JPanel quickPrompts = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
    private final List<AiAttachment> attachments = new ArrayList<AiAttachment>();
    private final AiAttachmentLoader attachmentLoader = new AiAttachmentLoader();
    private List<AiSessionSummary> allSessions = new ArrayList<AiSessionSummary>();
    private String lastSentText;
    private List<AiAttachment> lastSentAttachments = new ArrayList<AiAttachment>();
    private AiMode lastSentMode = AiMode.QA;
    private String lastCompletedRequestId;
    private String requestId;
    private AiMessageCard currentAssistant;
    private List<AiAnswerEvidence> currentEvidence = new ArrayList<AiAnswerEvidence>();
    private JScrollPane history;
    private JSplitPane conversationSplit;
    private JPanel sessionSidebar;
    private boolean loading;
    private boolean receivedChunk;
    private boolean awaitingConfirmation;
    private int requestGeneration;

    public AiChatPanel(AiAssistantClientService service) {
        this(service, null);
    }

    public AiChatPanel(AiAssistantClientService service,
                       PetActivityListener petActivity) {
        super("校园助手对话", "");
        this.service = service;
        this.petActivity = petActivity == null ? PetActivityListener.NONE : petActivity;
        setContent(content()); bind();
        loadSessions(null);
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 12)); root.setOpaque(false);
        JPanel tools = UiFactory.horizontal(8);
        JButton create = new SecondaryButton("新建会话");
        JButton clear = new SecondaryButton("归档会话");
        JButton rename = new SecondaryButton("重命名");
        JButton export = new SecondaryButton("导出");
        JButton refresh = new SecondaryButton("刷新");
        create.setActionCommand("create"); clear.setActionCommand("clear");
        refresh.setActionCommand("refresh");
        ActionListener actions = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ("create".equals(e.getActionCommand())) createSession();
                else if ("clear".equals(e.getActionCommand())) clearSession();
                else if ("rename".equals(e.getActionCommand())) renameSession();
                else if ("export".equals(e.getActionCommand())) exportTranscript();
                else loadSessions(selectedId());
            }
        };
        create.addActionListener(actions); clear.addActionListener(actions);
        refresh.addActionListener(actions);
        rename.setActionCommand("rename"); export.setActionCommand("export");
        rename.addActionListener(actions); export.addActionListener(actions);
        modes.setSelectedItem(AiMode.QA);
        modes.setToolTipText("问答读取校园实时数据；聊天自由交流；代办可执行需确认的校园操作");
        JButton toggleSidebar = new SecondaryButton("收起会话侧栏");
        toggleSidebar.addActionListener(e -> toggleSidebar(toggleSidebar));
        tools.add(toggleSidebar); tools.add(UiFactory.body("模式")); tools.add(modes);

        sessionSearch.setToolTipText("按会话标题搜索");
        JPanel sideTop = new JPanel(new BorderLayout(0, 6)); sideTop.setOpaque(false);
        sideTop.add(UiFactory.body("会话历史"), BorderLayout.NORTH);
        sideTop.add(sessionSearch, BorderLayout.SOUTH);
        sessions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessions.setFixedCellHeight(38);
        JPanel sideActions = new JPanel(new GridLayout(0, 2, 5, 5)); sideActions.setOpaque(false);
        sideActions.add(create); sideActions.add(rename); sideActions.add(export);
        sideActions.add(clear); sideActions.add(refresh);
        sessionSidebar = new JPanel(new BorderLayout(0, 8)); sessionSidebar.setOpaque(false);
        sessionSidebar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        sessionSidebar.setPreferredSize(new Dimension(235, 420));
        sessionSidebar.add(sideTop, BorderLayout.NORTH);
        sessionSidebar.add(new JScrollPane(sessions), BorderLayout.CENTER);
        sessionSidebar.add(sideActions, BorderLayout.SOUTH);

        messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));
        messageList.setBackground(new Color(0xFA, 0xFB, 0xF8));
        messageList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        history = new JScrollPane(messageList); history.setBorder(null);
        history.getVerticalScrollBar().setUnitIncrement(18);
        conversationSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sessionSidebar, history);
        conversationSplit.setBorder(null); conversationSplit.setResizeWeight(0.0);
        conversationSplit.setDividerLocation(235);
        JPanel composer = new JPanel(new BorderLayout(8, 6)); composer.setOpaque(false);
        JScrollPane promptScroll = new JScrollPane(quickPrompts,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        promptScroll.setBorder(null); promptScroll.setOpaque(false);
        promptScroll.getViewport().setOpaque(false); promptScroll.setPreferredSize(new Dimension(100, 46));
        quickPrompts.setOpaque(false);
        composer.add(promptScroll, BorderLayout.NORTH);
        JPanel inputRow = new JPanel(new BorderLayout(8, 0)); inputRow.setOpaque(false);
        attach.setToolTipText("聊天模式可上传图片、PDF、DOCX、PPTX、表格、文本和代码（最多 3 个）");
        attach.setPreferredSize(new Dimension(46, 40));
        inputRow.add(attach, BorderLayout.WEST);
        JPanel inputStack = new JPanel(new BorderLayout(0, 3)); inputStack.setOpaque(false);
        inputStack.add(new JScrollPane(input), BorderLayout.CENTER);
        attachmentChips.setOpaque(false); inputStack.add(attachmentChips, BorderLayout.SOUTH);
        inputRow.add(inputStack, BorderLayout.CENTER);
        composer.add(inputRow, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(3, 1, 0, 6)); buttons.setOpaque(false);
        buttons.add(send); buttons.add(cancel); buttons.add(retry); composer.add(buttons, BorderLayout.EAST);
        JPanel feedback = UiFactory.horizontal(6); feedback.add(UiFactory.muted("评价最近回答"));
        feedback.add(helpful); feedback.add(unhelpful); feedback.add(correct);
        composer.add(feedback, BorderLayout.SOUTH);
        cancel.setEnabled(false); retry.setEnabled(false);
        helpful.setEnabled(false); unhelpful.setEnabled(false); correct.setEnabled(false);
        root.add(tools, BorderLayout.NORTH); root.add(conversationSplit, BorderLayout.CENTER);
        root.add(composer, BorderLayout.SOUTH);
        updateModeControls(); return root;
    }

    private void bind() {
        sessions.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !loading) loadHistory(selectedId());
        });
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { send(); }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (requestId != null) service.cancel(requestId);
                requestGeneration++;
                awaitingConfirmation = false;
                petActivity.onMood(PetMood.IDLE, "", 0L);
                finish("回答已停止。", false);
            }
        });
        attach.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { chooseAttachments(); }
        });
        retry.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { send(); }
        });
        helpful.addActionListener(e -> feedback(true));
        unhelpful.addActionListener(e -> feedback(false));
        correct.addActionListener(e -> correction());
        sessionSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterSessions(); }
            public void removeUpdate(DocumentEvent e) { filterSessions(); }
            public void changedUpdate(DocumentEvent e) { filterSessions(); }
        });
        modes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { updateModeControls(); }
        });
        InputMap inputMap = input.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = input.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-message");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK), "insert-break");
        actionMap.put("send-message", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { send(); }
        });
        actionMap.put("insert-break", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { input.replaceSelection("\n"); }
        });
    }

    private void send() {
        final List<AiAttachment> outgoing = new ArrayList<AiAttachment>(attachments);
        String typed = input.getText().trim();
        if ((typed.isEmpty() && outgoing.isEmpty()) || requestId != null) return;
        final String text = typed.isEmpty() ? "请分析附件内容。" : typed;
        final AiMode mode = (AiMode) modes.getSelectedItem();
        lastSentText = text; lastSentAttachments = new ArrayList<AiAttachment>(outgoing);
        lastSentMode = mode; retry.setEnabled(false);
        helpful.setEnabled(false); unhelpful.setEnabled(false); correct.setEnabled(false);
        lastCompletedRequestId = null;
        String display = text;
        if (!outgoing.isEmpty()) display += "\n附件：" + attachmentNames(outgoing);
        append("我", display); input.setText(""); clearAttachments();
        send.setEnabled(false); cancel.setEnabled(true);
        currentAssistant = addCard(AiMessageCard.Kind.ASSISTANT, "");
        appendTranscriptPrefix("校园助手"); currentEvidence = new ArrayList<AiAnswerEvidence>();
        receivedChunk = false;
        awaitingConfirmation = false;
        final int generation = ++requestGeneration;
        petActivity.onMood(PetMood.THINKING, "让我查查～", 0L);
        requestId = service.query(selectedId(), text, mode, outgoing, new AiConversationListener() {
            public void onChunk(final String value) {
                ui(new Runnable() { public void run() {
                    if (generation != requestGeneration) return;
                    if (!receivedChunk) {
                        receivedChunk = true;
                        petActivity.onConnectivityChanged(true);
                        petActivity.onMood(PetMood.TALKING, "正在回答…", 0L);
                    }
                    appendChunk(value);
                }});
            }
            public void onComplete() {
                ui(new Runnable() { public void run() {
                    if (generation != requestGeneration) return;
                    if (!awaitingConfirmation) {
                        petActivity.onMood(PetMood.SUCCESS, "回答完成啦", 1800L);
                    }
                    petActivity.onConnectivityChanged(true);
                    lastCompletedRequestId = requestId;
                    completeCurrentAssistant();
                    helpful.setEnabled(true); unhelpful.setEnabled(true); correct.setEnabled(true);
                    finish(null, true);
                }});
            }
            public void onFailure(final String value) {
                ui(new Runnable() { public void run() {
                    if (generation != requestGeneration) return;
                    if (networkFailure(value)) petActivity.onConnectivityChanged(false);
                    else petActivity.onMood(PetMood.ERROR, "遇到点问题", 2600L);
                    restoreDraft(text, outgoing, mode); retry.setEnabled(true);
                    completeCurrentAssistant();
                    finish(value, false);
                }});
            }
            public void onActionRequired(final AiPendingAction action) {
                ui(new Runnable() { public void run() {
                    if (generation != requestGeneration) return;
                    awaitingConfirmation = true;
                    if (currentAssistant != null && currentAssistant.getText().trim().isEmpty()) {
                        String pending = "【待确认操作】\n" + action.getSummary();
                        currentAssistant.append(pending); transcriptText.append(pending);
                    }
                    petActivity.onMood(PetMood.ATTENTION, "需要你确认", 0L);
                    confirm(action);
                }});
            }
            public void onEvidence(final List<AiAnswerEvidence> evidence) {
                ui(new Runnable() { public void run() {
                    if (generation == requestGeneration) {
                        currentEvidence = new ArrayList<AiAnswerEvidence>(evidence);
                    }
                }});
            }
        });
    }

    private void chooseAttachments() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("选择发送给 AI 的图片或文档（最多 3 个）");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File[] selected = chooser.getSelectedFiles();
        if (selected == null || selected.length == 0) selected = new File[] {chooser.getSelectedFile()};
        if (attachments.size() + selected.length > 3) {
            JOptionPane.showMessageDialog(this, "一次最多上传 3 个附件。", "附件过多",
                    JOptionPane.WARNING_MESSAGE); return;
        }
        try {
            for (File file : selected) attachments.add(attachmentLoader.load(file));
            updateAttachmentInfo();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "附件无法添加",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateModeControls() {
        AiMode mode = (AiMode) modes.getSelectedItem();
        boolean chat = mode == AiMode.CHAT;
        attach.setVisible(chat); attach.setEnabled(chat && requestId == null);
        if (!chat) clearAttachments();
        quickPrompts.removeAll();
        String[] values = mode == AiMode.TASK
                ? new String[] {"帮我借《Java 编程思想》", "把矿泉水加入购物车", "报名数学建模竞赛", "帮我预约明天下午的自习室"}
                : mode == AiMode.CHAT
                ? new String[] {"帮我润色这段话", "解释一个编程概念", "根据图片描述内容", "帮我制定本周学习计划"}
                : new String[] {"我的学院是什么？", "图书馆里有什么书？", "我报名了哪些竞赛？", "宿舍有哪些用电规定？", "如何在系统里选课？"};
        for (final String value : values) {
            JButton button = new SecondaryButton(value);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (requestId == null) { input.setText(value); send(); }
                }
            });
            quickPrompts.add(button);
        }
        quickPrompts.revalidate(); quickPrompts.repaint();
    }

    private void clearAttachments() { attachments.clear(); updateAttachmentInfo(); }
    private void updateAttachmentInfo() {
        attachmentChips.removeAll();
        for (int index = 0; index < attachments.size(); index++) {
            final int removeIndex = index;
            JButton chip = new SecondaryButton(attachments.get(index).getFileName() + " ×");
            chip.setToolTipText("移除此附件");
            chip.addActionListener(e -> {
                if (removeIndex < attachments.size()) attachments.remove(removeIndex);
                updateAttachmentInfo();
            });
            attachmentChips.add(chip);
        }
        attachmentChips.revalidate(); attachmentChips.repaint();
    }
    private String attachmentNames(List<AiAttachment> values) {
        StringBuilder out = new StringBuilder();
        for (AiAttachment value : values) {
            if (out.length() > 0) out.append("、");
            out.append(value.getFileName());
        }
        return out.toString();
    }

    private void confirm(final AiPendingAction action) {
        Object[] options = new Object[]{"确认执行", "返回修改", "取消"};
        int choice = JOptionPane.showOptionDialog(this, action.getSummary()
                + "\n\n请核对对象、数量和时间；确认后由原业务模块执行，5 分钟内有效。",
                "确认业务操作", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        final boolean agreed = choice == 0;
        final boolean modify = choice == 1;
        new SwingWorker<AiConfirmResult, Void>() {
            private Exception failure;
            protected AiConfirmResult doInBackground() {
                try { return service.confirm(action.getActionId(), agreed); }
                catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                awaitingConfirmation = false;
                try {
                    if (failure != null) {
                        append("系统", failure.getMessage());
                        petActivity.onMood(PetMood.ERROR, "操作没有完成", 2600L);
                    } else {
                        append("校园助手", modify ? "已取消待办，请修改输入后重新发送。" : get().getContent());
                        if (modify) restoreDraft(lastSentText, lastSentAttachments, lastSentMode);
                        petActivity.onMood(agreed ? PetMood.SUCCESS : PetMood.IDLE,
                                agreed ? "办好啦！" : "", agreed ? 1800L : 0L);
                    }
                } catch (Exception ex) {
                    append("系统", "确认结果读取失败。");
                    petActivity.onMood(PetMood.ERROR, "操作没有完成", 2600L);
                }
            }
        }.execute();
    }

    private void finish(String message, boolean reload) {
        if (message != null) append("系统", message);
        requestId = null; send.setEnabled(true); cancel.setEnabled(false);
        attach.setEnabled(modes.getSelectedItem() == AiMode.CHAT);
        if (reload) loadSessions(selectedId(), false);
    }

    private void createSession() {
        task(new Work<String>() { public String run() throws Exception {
            return service.createSession();
        }}, new Result<String>() { public void accept(String id) { loadSessions(id); }});
    }

    private void clearSession() {
        final String id = selectedId(); if (id == null) return;
        if (JOptionPane.showConfirmDialog(this, "归档后该会话不再显示，是否继续？",
                "归档会话", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        task(new Work<Boolean>() { public Boolean run() throws Exception {
            service.clearSession(id); return Boolean.TRUE;
        }}, new Result<Boolean>() { public void accept(Boolean ignored) {
            clearMessages(); loadSessions(null);
        }});
    }

    private void loadSessions(final String preferred) {
        loadSessions(preferred, true);
    }

    private void loadSessions(final String preferred, final boolean loadHistory) {
        task(new Work<List<AiSessionSummary>>() { public List<AiSessionSummary> run()
                throws Exception { return service.sessions(); }},
                new Result<List<AiSessionSummary>>() {
            public void accept(List<AiSessionSummary> values) {
                allSessions = new ArrayList<AiSessionSummary>(values);
                renderSessions(preferred, loadHistory);
            }
        });
    }

    private void filterSessions() { if (!loading) renderSessions(selectedId(), true); }

    private void renderSessions(String preferred, boolean loadHistory) {
        String query = sessionSearch.getText().trim().toLowerCase();
        loading = true; sessionModel.clear(); AiSessionSummary selected = null;
        for (AiSessionSummary value : allSessions) {
            if (!query.isEmpty() && !value.toString().toLowerCase().contains(query)) continue;
            sessionModel.addElement(value);
            if (preferred != null && preferred.equals(value.getSessionId())) selected = value;
        }
        if (selected != null) sessions.setSelectedValue(selected, true);
        else if (!sessionModel.isEmpty()) sessions.setSelectedIndex(0);
        loading = false; if (loadHistory) loadHistory(selectedId());
    }

    private void renameSession() {
        final String id = selectedId(); if (id == null) return;
        String value = JOptionPane.showInputDialog(this, "新的会话标题（最多 80 字）：",
                sessions.getSelectedValue().toString());
        if (value == null || value.trim().isEmpty()) return;
        final String title = value.trim();
        task(new Work<Boolean>() { public Boolean run() throws Exception {
            service.renameSession(id, title); return Boolean.TRUE;
        }}, new Result<Boolean>() { public void accept(Boolean ignored) { loadSessions(id); }});
    }

    private void exportTranscript() {
        if (transcriptText.toString().trim().isEmpty()) return;
        JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File("校园助手会话.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.write(chooser.getSelectedFile().toPath(),
                    transcriptText.toString().getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "会话已导出。");
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage()); }
    }

    private void restoreDraft(String text, List<AiAttachment> files, AiMode mode) {
        if (text != null && (input.getText().trim().isEmpty() || input.getText().equals(text))) {
            input.setText("请分析附件内容。".equals(text) ? "" : text);
        }
        modes.setSelectedItem(mode); attachments.clear();
        if (files != null) attachments.addAll(files); updateAttachmentInfo();
    }

    private void feedback(final boolean positive) {
        final String sessionId = selectedId();
        if (sessionId == null || lastCompletedRequestId == null) return;
        String category = "GENERAL"; String comment = null;
        if (!positive) {
            JComboBox<String> categories = new JComboBox<String>(new String[]{
                    "回答不准确", "没有理解问题", "没有正确执行", "结果格式混乱", "缺少知识", "其他"});
            JTextArea details = UiFactory.textArea(4, 34);
            JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.add(categories, BorderLayout.NORTH);
            panel.add(new JScrollPane(details), BorderLayout.CENTER);
            if (JOptionPane.showConfirmDialog(this, panel, "这次回答哪里需要改进？",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            category = (String) categories.getSelectedItem(); comment = details.getText().trim();
        }
        AiFeedbackRequest request = new AiFeedbackRequest(sessionId, lastCompletedRequestId,
                positive ? "HELPFUL" : "UNHELPFUL", category, comment);
        saveFeedback(request);
    }

    private void loadHistory(final String id) {
        if (id == null) { clearMessages(); return; }
        task(new Work<List<AiChatMessage>>() { public List<AiChatMessage> run()
                throws Exception { return service.history(id); }},
                new Result<List<AiChatMessage>>() {
            public void accept(List<AiChatMessage> values) {
                clearMessages();
                for (AiChatMessage m : values) append(
                        "USER".equals(m.getSenderType()) ? "我" : "校园助手", m.getContent());
            }
        });
    }

    private String selectedId() {
        Object value = sessions.getSelectedValue();
        return value instanceof AiSessionSummary ? ((AiSessionSummary) value).getSessionId() : null;
    }
    private AiMessageCard append(String role, String text) {
        AiMessageCard.Kind kind = "我".equals(role) ? AiMessageCard.Kind.USER
                : "系统".equals(role) ? AiMessageCard.Kind.SYSTEM : AiMessageCard.Kind.ASSISTANT;
        AiMessageCard card = addCard(kind, text);
        appendTranscriptPrefix(role); transcriptText.append(text == null ? "" : text);
        card.complete(this::submitParameters, java.util.Collections.<AiAnswerEvidence>emptyList());
        return card;
    }
    private void appendChunk(String text) {
        if (currentAssistant != null) currentAssistant.append(text);
        transcriptText.append(text); scrollToBottom();
    }

    private AiMessageCard addCard(AiMessageCard.Kind kind, String text) {
        AiMessageCard card = new AiMessageCard(kind, text);
        messageList.add(card); messageList.add(Box.createVerticalStrut(9));
        messageList.revalidate(); messageList.repaint(); scrollToBottom(); return card;
    }

    private void completeCurrentAssistant() {
        if (currentAssistant == null) return;
        if (currentAssistant.getText().trim().isEmpty()) {
            messageList.remove(currentAssistant);
            int last = messageList.getComponentCount() - 1;
            if (last >= 0) messageList.remove(last);
            messageList.revalidate(); messageList.repaint();
        } else currentAssistant.complete(this::submitParameters, currentEvidence);
        currentAssistant = null; currentEvidence = new ArrayList<AiAnswerEvidence>();
        scrollToBottom();
    }

    private void submitParameters(String values) {
        if (requestId != null) return;
        modes.setSelectedItem(AiMode.TASK); input.setText(values); send();
    }

    private void appendTranscriptPrefix(String role) {
        if (transcriptText.length() > 0) transcriptText.append("\n\n");
        transcriptText.append(role).append("：");
    }

    private void clearMessages() {
        messageList.removeAll(); transcriptText.setLength(0); currentAssistant = null;
        messageList.revalidate(); messageList.repaint();
    }

    private void scrollToBottom() {
        if (history == null) return;
        SwingUtilities.invokeLater(() -> history.getVerticalScrollBar().setValue(
                history.getVerticalScrollBar().getMaximum()));
    }

    private void toggleSidebar(JButton toggle) {
        boolean visible = sessionSidebar.isVisible(); sessionSidebar.setVisible(!visible);
        conversationSplit.setDividerSize(visible ? 0 : 8);
        if (!visible) conversationSplit.setDividerLocation(235);
        toggle.setText(visible ? "展开会话侧栏" : "收起会话侧栏");
        conversationSplit.revalidate();
    }

    private void correction() {
        final String sessionId = selectedId();
        if (sessionId == null || lastCompletedRequestId == null) return;
        JTextArea details = UiFactory.textArea(5, 38);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(details),
                "请写出正确内容或期望的处理方式", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION || details.getText().trim().isEmpty()) return;
        saveFeedback(new AiFeedbackRequest(sessionId, lastCompletedRequestId,
                "UNHELPFUL", "用户纠错", details.getText().trim()));
    }

    private void saveFeedback(final AiFeedbackRequest request) {
        task(new Work<Boolean>() { public Boolean run() throws Exception {
            service.saveFeedback(request); return Boolean.TRUE;
        }}, new Result<Boolean>() { public void accept(Boolean ignored) {
            helpful.setEnabled(false); unhelpful.setEnabled(false); correct.setEnabled(false);
            append("系统", "感谢反馈，这会帮助管理员改进知识和工具。");
        }});
    }

    private boolean networkFailure(String value) {
        return value != null && (value.contains("连接中断") || value.contains("网络连接")
                || value.contains("无法连接") || value.contains("Connection"));
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
                    if (failure != null) {
                        if (networkFailure(failure.getMessage())) petActivity.onConnectivityChanged(false);
                        append("系统", failure.getMessage());
                    } else {
                        petActivity.onConnectivityChanged(true); result.accept(get());
                    }
                } catch (Exception ex) { append("系统", "AI 服务响应失败。"); }
            }
        }.execute();
    }
    private interface Work<T> { T run() throws Exception; }
    private interface Result<T> { void accept(T value); }
}
