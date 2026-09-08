package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
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
import java.util.UUID;

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
    private final JPanel quickPrompts = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
    private final UploadProgress uploadProgress = new UploadProgress();
    private final JLabel uploadStatus = UiFactory.muted("");
    private final List<AiAttachment> attachments = new ArrayList<AiAttachment>();
    private final AiAttachmentLoader attachmentLoader = new AiAttachmentLoader();
    private final List<JComponent> sessionControls = new ArrayList<JComponent>();
    private List<AiSessionSummary> allSessions = new ArrayList<AiSessionSummary>();
    private String lastSentText;
    private List<AiAttachment> lastSentAttachments = new ArrayList<AiAttachment>();
    private AiMode lastSentMode = AiMode.QA;
    private String requestId;
    private AiMessageCard currentAssistant;
    private AiMessageCard failedAssistantCard;
    private String failedRequestId;
    private List<AiAnswerEvidence> currentEvidence = new ArrayList<AiAnswerEvidence>();
    private JScrollPane history;
    private JSplitPane conversationSplit;
    private JPanel sessionSidebar;
    private boolean loading;
    private boolean receivedChunk;
    private boolean awaitingConfirmation;
    private boolean uploading;
    private int requestGeneration;

    public AiChatPanel(AiAssistantClientService service) {
        this(service, null);
    }

    public AiChatPanel(AiAssistantClientService service,
                       PetActivityListener petActivity) {
        super("", "");
        this.service = service;
        this.petActivity = petActivity == null ? PetActivityListener.NONE : petActivity;
        removeConversationHeading();
        setContent(content()); bind();
        loadSessions(null);
    }

    private void removeConversationHeading() {
        BorderLayout layout = (BorderLayout) getLayout();
        Component heading = layout.getLayoutComponent(BorderLayout.NORTH);
        if (heading != null) remove(heading);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 12)); root.setOpaque(false);
        JPanel tools = UiFactory.horizontal(8);
        JButton create = new SecondaryButton("新建会话");
        JButton clear = new SecondaryButton("归档会话");
        JButton rename = new SecondaryButton("重命名");
        JButton export = new SecondaryButton("导出");
        JButton refresh = new SecondaryButton("刷新");
        JButton archived = new SecondaryButton("已归档");
        create.setActionCommand("create"); clear.setActionCommand("clear");
        refresh.setActionCommand("refresh");
        ActionListener actions = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ("create".equals(e.getActionCommand())) createSession();
                else if ("clear".equals(e.getActionCommand())) clearSession();
                else if ("rename".equals(e.getActionCommand())) renameSession();
                else if ("export".equals(e.getActionCommand())) exportTranscript();
                else if ("archived".equals(e.getActionCommand())) showArchivedSessions();
                else loadSessions(selectedId());
            }
        };
        create.addActionListener(actions); clear.addActionListener(actions);
        refresh.addActionListener(actions);
        rename.setActionCommand("rename"); export.setActionCommand("export");
        archived.setActionCommand("archived");
        rename.addActionListener(actions); export.addActionListener(actions);
        archived.addActionListener(actions);
        modes.setSelectedItem(AiMode.QA);
        modes.setToolTipText("问答读取校园实时数据；聊天自由交流；代办可执行需确认的校园操作");
        JButton toggleSidebar = new SecondaryButton("展开会话侧栏");
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
        sideActions.add(archived);
        sessionControls.add(create); sessionControls.add(clear); sessionControls.add(rename);
        sessionControls.add(export); sessionControls.add(refresh); sessionControls.add(archived);
        sessionControls.add(sessionSearch); sessionControls.add(sessions);
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
        sessionSidebar.setVisible(false);
        conversationSplit.setDividerSize(0);
        conversationSplit.setDividerLocation(0);
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
        cancel.setEnabled(false); retry.setEnabled(false);
        root.add(tools, BorderLayout.NORTH); root.add(conversationSplit, BorderLayout.CENTER);
        root.add(composer, BorderLayout.SOUTH);
        updateModeControls(); return root;
    }

    private void bind() {
        sessions.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !loading) loadHistory(selectedId());
        });
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { send(false); }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (requestId != null) service.cancel(requestId);
                requestGeneration++;
                awaitingConfirmation = false;
                petActivity.onMood(PetMood.IDLE, "", 0L);
                completeCurrentAssistant(null);
                finish("回答已停止。", false);
            }
        });
        attach.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { chooseAttachments(); }
        });
        retry.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { send(true); }
        });
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
            public void actionPerformed(ActionEvent e) { send(false); }
        });
        actionMap.put("insert-break", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { input.replaceSelection("\n"); }
        });
    }

    private void send(boolean retrying) {
        if (uploading) {
            JOptionPane.showMessageDialog(this, "附件仍在处理，请等进度圈完成后再发送。");
            return;
        }
        if (requestId != null || (retrying && failedRequestId == null)) return;
        final List<AiAttachment> outgoing = retrying
                ? new ArrayList<AiAttachment>(lastSentAttachments)
                : new ArrayList<AiAttachment>(attachments);
        String typed = retrying ? lastSentText : input.getText().trim();
        if (!retrying && typed.isEmpty() && outgoing.isEmpty()) return;
        final String text = typed == null || typed.isEmpty() ? "请分析附件内容。" : typed;
        final AiMode mode = retrying ? lastSentMode : (AiMode) modes.getSelectedItem();
        if (!retrying) {
            lastSentText = text; lastSentAttachments = new ArrayList<AiAttachment>(outgoing);
            lastSentMode = mode;
            String display = text;
            if (!outgoing.isEmpty()) display += "\n附件：" + attachmentNames(outgoing);
            append("我", display);
        } else {
            removeCard(failedAssistantCard); failedAssistantCard = null;
        }
        input.setText(""); clearAttachments(); retry.setEnabled(false);
        send.setEnabled(false); attach.setEnabled(false); modes.setEnabled(false); cancel.setEnabled(true);
        currentAssistant = addCard(AiMessageCard.Kind.ASSISTANT, "");
        appendTranscriptPrefix("校园助手"); currentEvidence = new ArrayList<AiAnswerEvidence>();
        receivedChunk = false;
        awaitingConfirmation = false;
        final int generation = ++requestGeneration;
        final String logicalRequestId = retrying ? failedRequestId : UUID.randomUUID().toString();
        requestId = logicalRequestId; updateComposerState();
        petActivity.onMood(PetMood.THINKING, "让我查查～", 0L);
        String startedRequestId = service.queryWithRequestId(logicalRequestId, selectedId(), text,
                mode, outgoing, new AiConversationListener() {
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
                    failedRequestId = null; failedAssistantCard = null; retry.setEnabled(false);
                    completeCurrentAssistant(requestId);
                    finish(null, true);
                }});
            }
            public void onFailure(final String value) {
                ui(new Runnable() { public void run() {
                    if (generation != requestGeneration) return;
                    if (networkFailure(value)) petActivity.onConnectivityChanged(false);
                    else petActivity.onMood(PetMood.ERROR, "遇到点问题", 2600L);
                    restoreDraft(text, outgoing, mode); retry.setEnabled(true);
                    if (currentAssistant != null) currentAssistant.append(
                            (currentAssistant.getText().trim().isEmpty() ? "" : "\n\n")
                                    + "发送失败：" + value);
                    failedAssistantCard = currentAssistant;
                    failedRequestId = logicalRequestId;
                    completeCurrentAssistant(null);
                    finish(null, false);
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
        if (requestId != null && generation == requestGeneration
                && startedRequestId != null && !startedRequestId.trim().isEmpty()) {
            requestId = startedRequestId;
        }
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
        final File[] files = selected;
        uploading = true;
        uploadStatus.setText("正在处理：" + files[0].getName());
        updateAttachmentInfo(); updateComposerState();
        new SwingWorker<List<AiAttachment>, String>() {
            private Exception failure;
            protected List<AiAttachment> doInBackground() {
                List<AiAttachment> loaded = new ArrayList<AiAttachment>();
                try {
                    for (File file : files) {
                        publish(file.getName());
                        loaded.add(attachmentLoader.load(file));
                    }
                } catch (Exception ex) { failure = ex; }
                return loaded;
            }
            protected void process(List<String> names) {
                if (!names.isEmpty()) uploadStatus.setText("正在处理：" + names.get(names.size() - 1));
            }
            protected void done() {
                uploading = false;
                try {
                    if (failure != null) {
                        JOptionPane.showMessageDialog(AiChatPanel.this, failure.getMessage(),
                                "附件无法添加", JOptionPane.WARNING_MESSAGE);
                    } else attachments.addAll(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AiChatPanel.this, "附件处理失败。",
                            "附件无法添加", JOptionPane.WARNING_MESSAGE);
                }
                updateAttachmentInfo(); updateComposerState();
            }
        }.execute();
    }

    private void updateModeControls() {
        AiMode mode = (AiMode) modes.getSelectedItem();
        boolean chat = mode == AiMode.CHAT;
        attach.setVisible(chat); attach.setEnabled(chat && requestId == null && !uploading);
        if (!chat) clearAttachments();
        quickPrompts.removeAll();
        String[] values = mode == AiMode.TASK
                ? new String[] {"帮我归还图书 软件工程实践导论", "把 VCampus纪念马克杯加入购物车",
                    "取消报名校园创新实践演示赛", "帮我预约自习室"}
                : mode == AiMode.CHAT
                ? new String[] {"帮我制定本周学习计划", "解释 Java 的多态",
                    "帮我润色一封请假邮件", "总结番茄工作法的优缺点"}
                : new String[] {"查看我的课表", "图书馆里有什么书？", "商店里有什么商品？",
                    "查询可用自习室", "我报名了哪些竞赛？"};
        for (final String value : values) {
            JButton button = new SecondaryButton(value);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (requestId == null) { input.setText(value); send(false); }
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
            JButton chip = new SecondaryButton("✓ " + attachments.get(index).getFileName() + " ×");
            chip.setToolTipText("移除此附件");
            chip.addActionListener(e -> {
                if (removeIndex < attachments.size()) attachments.remove(removeIndex);
                updateAttachmentInfo();
            });
            attachmentChips.add(chip);
        }
        if (uploading) {
            attachmentChips.add(uploadProgress);
            attachmentChips.add(uploadStatus);
        }
        attachmentChips.revalidate(); attachmentChips.repaint();
    }

    private void updateComposerState() {
        boolean idle = requestId == null && !uploading;
        send.setEnabled(idle);
        modes.setEnabled(idle);
        attach.setEnabled(idle && modes.getSelectedItem() == AiMode.CHAT);
        for (JComponent control : sessionControls) control.setEnabled(idle);
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
                        append("校园助手", modify ? "已取消待办，请修改输入后重新发送。" : get().getContent(),
                                action.getRequestId());
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
        requestId = null; cancel.setEnabled(false); updateComposerState();
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

    private void showArchivedSessions() {
        task(new Work<List<AiSessionSummary>>() { public List<AiSessionSummary> run() throws Exception {
            return service.archivedSessions();
        }}, new Result<List<AiSessionSummary>>() { public void accept(List<AiSessionSummary> values) {
            if (values.isEmpty()) { JOptionPane.showMessageDialog(AiChatPanel.this, "暂无已归档会话。"); return; }
            final JList<AiSessionSummary> archived = new JList<AiSessionSummary>(
                    values.toArray(new AiSessionSummary[values.size()]));
            archived.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); archived.setSelectedIndex(0);
            JScrollPane scroll = new JScrollPane(archived); scroll.setPreferredSize(new Dimension(420, 280));
            if (JOptionPane.showConfirmDialog(AiChatPanel.this, scroll, "选择要恢复的会话",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            final AiSessionSummary selected = archived.getSelectedValue(); if (selected == null) return;
            task(new Work<Boolean>() { public Boolean run() throws Exception {
                service.restoreSession(selected.getSessionId()); return Boolean.TRUE;
            }}, new Result<Boolean>() { public void accept(Boolean ignored) {
                loadSessions(selected.getSessionId());
            }});
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
        loading = false;
        // 初始/刷新会话列表可能晚于流式请求返回，生成期间绝不能用历史加载覆盖当前气泡。
        if (loadHistory && requestId == null) loadHistory(selectedId());
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

    private void feedback(final AiMessageCard card, final String targetRequestId,
                          final boolean positive) {
        final String sessionId = selectedId();
        if (sessionId == null || targetRequestId == null || targetRequestId.trim().isEmpty()) return;
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
        AiFeedbackRequest request = new AiFeedbackRequest(sessionId, targetRequestId,
                positive ? "HELPFUL" : "UNHELPFUL", category, comment);
        saveFeedback(request, card);
    }

    private void loadHistory(final String id) {
        failedRequestId = null; failedAssistantCard = null; retry.setEnabled(false);
        if (id == null) { clearMessages(); return; }
        task(new Work<List<AiChatMessage>>() { public List<AiChatMessage> run()
                throws Exception { return service.history(id); }},
                new Result<List<AiChatMessage>>() {
            public void accept(List<AiChatMessage> values) {
                clearMessages();
                for (int index = 0; index < values.size(); index++) {
                    AiChatMessage m = values.get(index);
                    boolean assistant = !"USER".equals(m.getSenderType());
                    append(assistant ? "校园助手" : "我", m.getContent(),
                            assistant && isLastAssistantForRequest(values, index)
                                    ? m.getRequestId() : null);
                }
            }
        });
    }

    private boolean isLastAssistantForRequest(List<AiChatMessage> values, int index) {
        String id = values.get(index).getRequestId();
        if (id == null || id.trim().isEmpty()) return false;
        for (int next = index + 1; next < values.size(); next++) {
            AiChatMessage message = values.get(next);
            if (!"USER".equals(message.getSenderType()) && id.equals(message.getRequestId())) {
                return false;
            }
        }
        return true;
    }

    private String selectedId() {
        Object value = sessions.getSelectedValue();
        return value instanceof AiSessionSummary ? ((AiSessionSummary) value).getSessionId() : null;
    }
    private AiMessageCard append(String role, String text) {
        return append(role, text, null);
    }

    private AiMessageCard append(String role, String text, String targetRequestId) {
        AiMessageCard.Kind kind = "我".equals(role) ? AiMessageCard.Kind.USER
                : "系统".equals(role) ? AiMessageCard.Kind.SYSTEM : AiMessageCard.Kind.ASSISTANT;
        AiMessageCard card = addCard(kind, text);
        appendTranscriptPrefix(role); transcriptText.append(text == null ? "" : text);
        card.complete(this::submitParameters, java.util.Collections.<AiAnswerEvidence>emptyList());
        if (kind == AiMessageCard.Kind.ASSISTANT && targetRequestId != null
                && !targetRequestId.trim().isEmpty() && text != null
                && !text.startsWith("【待确认操作】")) {
            installFeedback(card, targetRequestId);
        }
        return card;
    }
    private void appendChunk(String text) {
        if (currentAssistant != null) currentAssistant.append(text);
        transcriptText.append(text); scrollToBottom();
    }

    private AiMessageCard addCard(AiMessageCard.Kind kind, String text) {
        AiMessageCard card = new AiMessageCard(kind, text);
        JPanel row = new JPanel(new FlowLayout(kind == AiMessageCard.Kind.USER
                ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0)) {
            @Override public Dimension getMaximumSize() {
                Dimension preferred = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, preferred.height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(card);
        messageList.add(row); messageList.add(Box.createVerticalStrut(9));
        messageList.revalidate(); messageList.repaint(); scrollToBottom(); return card;
    }

    private void completeCurrentAssistant(String completedRequestId) {
        if (currentAssistant == null) return;
        if (currentAssistant.getText().trim().isEmpty()) {
            Container row = currentAssistant.getParent();
            messageList.remove(row);
            int last = messageList.getComponentCount() - 1;
            if (last >= 0) messageList.remove(last);
            messageList.revalidate(); messageList.repaint();
        } else {
            currentAssistant.complete(this::submitParameters, currentEvidence);
            if (completedRequestId != null
                    && !currentAssistant.getText().startsWith("【待确认操作】")) {
                installFeedback(currentAssistant, completedRequestId);
            }
        }
        currentAssistant = null; currentEvidence = new ArrayList<AiAnswerEvidence>();
        scrollToBottom();
    }

    private void submitParameters(String values) {
        if (requestId != null) return;
        modes.setSelectedItem(AiMode.TASK); input.setText(values); send(false);
    }

    private void appendTranscriptPrefix(String role) {
        if (transcriptText.length() > 0) transcriptText.append("\n\n");
        transcriptText.append(role).append("：");
    }

    private void clearMessages() {
        messageList.removeAll(); transcriptText.setLength(0); currentAssistant = null;
        messageList.revalidate(); messageList.repaint();
    }

    private void removeCard(AiMessageCard card) {
        if (card == null || card.getParent() == null) return;
        Component row = card.getParent(); int index = -1;
        for (int i = 0; i < messageList.getComponentCount(); i++) {
            if (messageList.getComponent(i) == row) { index = i; break; }
        }
        if (index >= 0) {
            messageList.remove(index);
            if (index < messageList.getComponentCount()) messageList.remove(index);
            messageList.revalidate(); messageList.repaint();
        }
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

    private void installFeedback(final AiMessageCard card, final String targetRequestId) {
        card.addFeedbackActions(() -> feedback(card, targetRequestId, true),
                () -> feedback(card, targetRequestId, false),
                () -> correction(card, targetRequestId));
    }

    private void correction(final AiMessageCard card, final String targetRequestId) {
        final String sessionId = selectedId();
        if (sessionId == null || targetRequestId == null || targetRequestId.trim().isEmpty()) return;
        JTextArea details = UiFactory.textArea(5, 38);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(details),
                "请写出正确内容或期望的处理方式", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION || details.getText().trim().isEmpty()) return;
        saveFeedback(new AiFeedbackRequest(sessionId, targetRequestId,
                "UNHELPFUL", "用户纠错", details.getText().trim()), card);
    }

    private void saveFeedback(final AiFeedbackRequest request, final AiMessageCard card) {
        task(new Work<Boolean>() { public Boolean run() throws Exception {
            service.saveFeedback(request); return Boolean.TRUE;
        }}, new Result<Boolean>() { public void accept(Boolean ignored) {
            card.markFeedbackSubmitted();
            append("系统", "感谢反馈，这会帮助管理员改进知识和工具。");
        }});
    }

    private boolean networkFailure(String value) {
        return value != null && (value.contains("连接中断") || value.contains("网络连接")
                || value.contains("无法连接") || value.contains("Connection"));
    }

    private void ui(Runnable work) { SwingUtilities.invokeLater(work); }

    /** 文件解析期间的轻量旋转进度圈。 */
    private static final class UploadProgress extends JComponent {
        private int angle;
        private final Timer timer = new Timer(70, e -> {
            angle = (angle + 24) % 360;
            repaint();
        });

        private UploadProgress() { setPreferredSize(new Dimension(20, 20)); }
        @Override public void addNotify() { super.addNotify(); timer.start(); }
        @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(DesignTokens.BORDER);
            g.drawOval(3, 3, 13, 13);
            g.setColor(DesignTokens.PRIMARY);
            g.drawArc(3, 3, 13, 13, angle, 105);
            g.dispose();
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
