package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.WrapLayout;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.ai.AiToolRouteTestResult;

import javax.swing.*;
import java.awt.*;

/** 安全验证自然语言到工具的路由，不执行实际校园业务。 */
public final class AiToolRouteTestPanel extends SectionCard {
    private final AiAssistantClientService service;
    private final JTextField question = UiFactory.textField(48);
    private final JTextArea result = UiFactory.textArea(12, 70);
    private final JLabel status = UiFactory.muted("");
    private final JButton runButton = new PrimaryButton("检查路由");
    private final JButton batchButton = new SecondaryButton("全工具回归（47 项）");
    private boolean running;

    public AiToolRouteTestPanel(AiAssistantClientService service) {
        super("工具路由测试", "验证问法是否命中预期工具，并查看缺失参数；测试绝不会执行工具。");
        this.service = service; result.setEditable(false); setContent(content());
    }
    private JComponent content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        JPanel input = new JPanel(new WrapLayout(8)); input.setOpaque(false);
        input.add(UiFactory.body("测试问法")); input.add(question);
        JButton run = runButton; input.add(run);
        input.add(batchButton); batchButton.addActionListener(e -> batch());
        JButton copy = new SecondaryButton("复制诊断报告");
        copy.addActionListener(e -> {
            try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(result.getText()), null); }
            catch (RuntimeException ex) { status.setText("无法访问剪贴板"); }
        }); input.add(copy);
        JPanel examples = new JPanel(new WrapLayout(8)); examples.setOpaque(false);
        for (String text : new String[]{"商店里有什么商品？", "图书馆里有什么书？", "帮我预约自习室"}) {
            JButton sample = new SecondaryButton(text); sample.addActionListener(e -> question.setText(text));
            examples.add(sample);
        }
        JPanel north = new JPanel(new BorderLayout(0, 8)); north.setOpaque(false);
        north.add(input, BorderLayout.NORTH); north.add(examples, BorderLayout.SOUTH);
        JScrollPane scroll = new JScrollPane(result); scroll.setPreferredSize(new Dimension(820, 350));
        run.addActionListener(e -> run()); question.addActionListener(e -> run());
        root.add(north, BorderLayout.NORTH); root.add(scroll, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH); return root;
    }
    private void run() {
        final String text = question.getText().trim(); if (text.isEmpty() || running) return;
        busy(true);
        status.setText("正在分析路由……"); result.setText("");
        new SwingWorker<AiToolRouteTestResult, Void>() {
            private Exception failure;
            protected AiToolRouteTestResult doInBackground() {
                try { return service.testToolRoute(text); } catch (Exception ex) { failure = ex; return null; }
            }
            protected void done() {
                try {
                    if (failure != null) { status.setText("测试失败：" + failure.getMessage()); return; }
                    AiToolRouteTestResult value = get();
                    if (!value.isMatched()) {
                        result.setText("路由结果：普通问答\n说明：" + safe(value.getClarification()));
                        status.setText("未命中校园工具"); return;
                    }
                    result.setText("路由结果：" + value.getToolName() + "\n能力：" + safe(value.getDescription())
                            + "\n操作类型：" + (value.isWriteOperation() ? "写操作（正式执行时需确认）" : "只读查询")
                            + "\n解析参数：" + safe(value.getArgumentsJson())
                            + "\n参数检查：" + (value.getClarification() == null ? "完整" : value.getClarification()));
                    status.setText(value.getClarification() == null ? "路由和参数均通过" : "已命中工具，但需要补充参数");
                } catch (Exception ex) { status.setText("路由响应读取失败"); }
                finally { busy(false); }
            }
        }.execute();
    }
    private void busy(boolean value) {
        running = value; runButton.setEnabled(!value); batchButton.setEnabled(!value);
    }
    private void batch() {
        if (running) return;
        busy(true); result.setText(""); status.setText("正在执行全工具回归；不会修改业务数据…");
        new SwingWorker<String, String>() {
            protected String doInBackground() {
                int passed = 0, failed = 0;
                for (String[] sample : edu.seu.vcampus.common.ai.AiRouteExamples.all()) {
                    try {
                        AiToolRouteTestResult actual = service.testToolRoute(sample[1]);
                        boolean ok = actual.isMatched() && sample[0].equals(actual.getToolName());
                        if (ok) passed++; else failed++;
                        publish((ok ? "通过" : "失败") + " | " + sample[1].replace('\n', ' ')
                                + "\n预期：" + sample[0] + " | 实际：" + actual.getToolName()
                                + "\n参数：" + safe(actual.getArgumentsJson())
                                + "\n补参：" + safe(actual.getClarification()) + "\n\n");
                    } catch (Exception ex) { failed++; publish("失败 | " + sample[0] + "：" + ex.getMessage() + "\n"); }
                }
                return "回归结束：通过 " + passed + " / 失败 " + failed
                        + "。这是路由检查；真实业务状态、权限和 API 连通性需结合工具状态与隔离环境测试。";
            }
            protected void process(java.util.List<String> chunks) {
                for (String text : chunks) result.append(text);
            }
            protected void done() {
                try { status.setText(get()); result.append(get()); result.setCaretPosition(0); }
                catch (Exception ex) { status.setText("批量诊断失败：" + ex.getMessage()); }
                finally { busy(false); }
            }
        }.execute();
    }
    private String safe(String value) { return value == null ? "" : value; }
}
