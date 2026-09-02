package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.AiMonitorSnapshot;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** 不暴露提示词、API Key 或会话 token 的 AI 运行监控摘要。 */
public final class AiMonitorPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final JLabel model = UiFactory.body("—");
    private final JLabel sessions = UiFactory.body("—");
    private final JLabel messages = UiFactory.body("—");
    private final JLabel knowledge = UiFactory.body("—");
    private final JLabel pending = UiFactory.body("—");
    private final JLabel tools = UiFactory.body("—");

    public AiMonitorPanel(AiAssistantClientService service) {
        super("AI 服务运行监控", "");
        this.service = service; setContent(content()); refresh();
    }

    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 12)); root.setOpaque(false);
        JPanel grid = new JPanel(new GridLayout(3, 2, 16, 12)); grid.setOpaque(false);
        grid.add(item("模型状态", model)); grid.add(item("活跃会话", sessions));
        grid.add(item("消息总数", messages)); grid.add(item("有效知识片段", knowledge));
        grid.add(item("待确认操作", pending)); grid.add(item("工具执行", tools));
        JButton refresh = new SecondaryButton("刷新运行状态");
        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refresh(); }
        });
        root.add(grid, BorderLayout.CENTER); root.add(refresh, BorderLayout.SOUTH); return root;
    }

    private JComponent item(String name, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(0, 4)); panel.setOpaque(false);
        panel.add(UiFactory.muted(name), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER); return panel;
    }

    private void refresh() {
        new SwingWorker<AiMonitorSnapshot, Void>() {
            private Exception failure;
            protected AiMonitorSnapshot doInBackground() {
                try { return service.monitor(); }
                catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) {
                        model.setText("读取失败：" + failure.getMessage()); return;
                    }
                    AiMonitorSnapshot snapshot = get();
                    model.setText((snapshot.isModelConfigured() ? "AI API 已配置" : "API 未配置，关键词降级")
                            + " / " + snapshot.getModelName());
                    sessions.setText(Long.toString(snapshot.getActiveSessions()));
                    messages.setText(Long.toString(snapshot.getMessages()));
                    knowledge.setText(Long.toString(snapshot.getKnowledgeChunks()));
                    pending.setText(Long.toString(snapshot.getPendingActions()));
                    tools.setText("成功 " + snapshot.getSucceededTools()
                            + " / 失败 " + snapshot.getFailedTools());
                } catch (Exception ex) { model.setText("读取失败"); }
            }
        }.execute();
    }
}
