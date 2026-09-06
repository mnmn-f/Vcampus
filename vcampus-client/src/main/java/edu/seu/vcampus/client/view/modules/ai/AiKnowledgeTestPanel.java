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
    private final JTextArea answer = UiFactory.textArea(10, 70);
    private final DefaultListModel<String> hits = new DefaultListModel<String>();
    private final JLabel status = UiFactory.muted("输入学生可能提出的问题，检查命中知识和回答。");

    public AiKnowledgeTestPanel(AiAssistantClientService service) {
        super("知识问答测试", "测试不会写入学生会话，也不会执行校园业务操作。");
        this.service = service; answer.setEditable(false); setContent(content());
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        JPanel bar = UiFactory.horizontal(8); bar.add(UiFactory.body("测试问题"));
        bar.add(question); JButton run = new PrimaryButton("运行测试"); bar.add(run);
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
                    status.setText((result.isModelUsed() ? "大模型回答" : "本地降级回答")
                            + " · 命中 " + result.getMatches().size() + " 条知识");
                } catch (Exception ex) { status.setText("测试响应读取失败"); }
            }
        }.execute();
    }
}
