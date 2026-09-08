package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.AiKnowledgeChunk;
import edu.seu.vcampus.common.ai.AiKnowledgeTestResult;

import javax.swing.*;
import java.awt.*;

/** 管理员发布知识前的只读检索与答案预览。 */
public final class AiKnowledgeTestPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final JTextField question = UiFactory.textField(48);
    private final JTextField expectedAnswer = UiFactory.textField(28);
    private final JTextField expectedHit = UiFactory.textField(20);
    private final JTextArea answer = UiFactory.textArea(10, 70);
    private final DefaultListModel<String> hits = new DefaultListModel<String>();
    private final JLabel status = UiFactory.muted("输入学生可能提出的问题，检查命中知识和回答。");

    public AiKnowledgeTestPanel(AiAssistantClientService service) {
        super("知识问答测试", "测试不会写入学生会话，也不会执行校园业务操作。");
        this.service = service; answer.setEditable(false); setContent(content());
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        JPanel bar = new JPanel(new GridBagLayout()); bar.setOpaque(false);
        GridBagConstraints q = UiFactory.gbc(0, 0); q.gridwidth = 2; q.weightx = 1;
        q.fill = GridBagConstraints.HORIZONTAL; bar.add(UiFactory.labelledField("测试问题", question), q);
        JButton run = new PrimaryButton("运行测试"); GridBagConstraints button = UiFactory.gbc(2, 0);
        bar.add(run, button);
        GridBagConstraints a = UiFactory.gbc(0, 1); a.weightx = 1; a.fill = GridBagConstraints.HORIZONTAL;
        bar.add(UiFactory.labelledField("标准答案关键词（多个用 | 分隔）", expectedAnswer), a);
        GridBagConstraints h = UiFactory.gbc(1, 1); h.weightx = 1; h.fill = GridBagConstraints.HORIZONTAL;
        bar.add(UiFactory.labelledField("预期命中知识标题", expectedHit), h);
        run.addActionListener(e -> runTest());
        question.addActionListener(e -> runTest());
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(answer), new JScrollPane(new JList<String>(hits)));
        split.setResizeWeight(.7); split.setPreferredSize(new Dimension(780, 430));
        root.add(bar, BorderLayout.NORTH); root.add(split, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH); return root;
    }

    private void runTest() {
        final String value = question.getText().trim(); if (value.isEmpty()) return;
        status.setText("正在检索并生成预览……"); answer.setText(""); hits.clear();
        new SwingWorker<AiKnowledgeTestResult, Void>() {
            private Exception failure;
            protected AiKnowledgeTestResult doInBackground() {
                try { return service.testKnowledge(value); }
                catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) { status.setText("测试失败：" + failure.getMessage()); return; }
                    AiKnowledgeTestResult result = get(); answer.setText(result.getAnswer());
                    int index = 1;
                    for (AiKnowledgeChunk hit : result.getMatches()) {
                        hits.addElement(index++ + ". " + hit.getTitle() + "（" + hit.getSourceType() + "）");
                    }
                    boolean answerPass = matchesAnswer(result.getAnswer(), expectedAnswer.getText());
                    boolean hitPass = matchesHit(result, expectedHit.getText());
                    status.setText((answerPass && hitPass ? "回归通过" : "回归未通过") + " · "
                            + (result.isModelUsed() ? "大模型回答" : "本地降级回答")
                            + " · 命中 " + result.getMatches().size() + " 条知识"
                            + (!answerPass ? " · 标准答案关键词未满足" : "")
                            + (!hitPass ? " · 未命中预期知识" : ""));
                } catch (Exception ex) { status.setText("测试响应读取失败"); }
            }
        }.execute();
    }

    static boolean matchesAnswer(String answer, String expected) {
        String clean = expected == null ? "" : expected.trim(); if (clean.isEmpty()) return true;
        String actual = answer == null ? "" : answer;
        for (String token : clean.split("\\|")) if (!token.trim().isEmpty()
                && !actual.contains(token.trim())) return false;
        return true;
    }
    static boolean matchesHit(AiKnowledgeTestResult result, String expected) {
        String clean = expected == null ? "" : expected.trim(); if (clean.isEmpty()) return true;
        for (AiKnowledgeChunk hit : result.getMatches()) if (hit.getTitle() != null
                && hit.getTitle().contains(clean)) return true;
        return false;
    }
}
