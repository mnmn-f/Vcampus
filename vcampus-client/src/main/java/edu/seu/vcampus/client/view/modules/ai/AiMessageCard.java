package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.common.ai.AiAnswerEvidence;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/** 对话中的消息气泡；可升级为业务卡、参数卡并附带折叠依据。 */
final class AiMessageCard extends JPanel {
    enum Kind { USER, ASSISTANT, SYSTEM }

    private final JLabel heading = UiFactory.body("");
    private final JTextArea body = UiFactory.textArea(1, 56);
    private final JPanel extras = new JPanel();
    private final StringBuilder text = new StringBuilder();
    private Color accent = DesignTokens.PRIMARY_BORDER;

    AiMessageCard(Kind kind, String initial) {
        super(new BorderLayout(0, 7));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        setBackground(kind == Kind.USER ? DesignTokens.PRIMARY_LIGHT : Color.WHITE);
        setOpaque(true);
        heading.setFont(DesignTokens.medium(13));
        heading.setText(kind == Kind.USER ? "我" : kind == Kind.SYSTEM ? "系统提示" : "校园助手");
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.add(heading);
        if (kind != Kind.USER) {
            JButton copy = new SecondaryButton("复制");
            copy.setMargin(new Insets(2, 8, 2, 8));
            copy.addActionListener(e -> copy()); top.add(copy, BorderLayout.EAST);
        }
        body.setEditable(false); body.setOpaque(false); body.setLineWrap(true);
        body.setWrapStyleWord(true); body.setBorder(null);
        extras.setOpaque(false); extras.setLayout(new BoxLayout(extras, BoxLayout.Y_AXIS));
        add(top, BorderLayout.NORTH); add(body, BorderLayout.CENTER); add(extras, BorderLayout.SOUTH);
        append(initial); setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    void append(String value) {
        if (value == null || value.isEmpty()) return;
        text.append(value); body.setText(text.toString()); resizeBody();
    }

    String getText() { return text.toString(); }

    void complete(Consumer<String> parameterSubmit, List<AiAnswerEvidence> evidence) {
        String value = getText().trim();
        if (value.startsWith("【实时数据｜")) makeBusiness(value);
        else if (value.startsWith("【待确认操作】")) makePending(value);
        else if (value.startsWith("【还需要一点信息】")) makeParameters(value, parameterSubmit);
        addEvidence(evidence); revalidate(); repaint();
    }

    private void makeBusiness(String value) {
        int close = value.indexOf('】');
        if (close > 0) {
            heading.setText("业务结果 · " + value.substring("【实时数据｜".length(), close));
            replaceBody(value.substring(close + 1).trim());
        } else heading.setText("业务结果");
        accent = DesignTokens.GOLD; setBackground(new Color(0xFF, 0xFC, 0xF3)); restyle();
    }

    private void makeParameters(String value, Consumer<String> submit) {
        heading.setText("待补充参数");
        replaceBody(value.replace("【还需要一点信息】", "").trim());
        accent = DesignTokens.WARNING; setBackground(DesignTokens.WARNING_BACKGROUND); restyle();
        String[] labels = fieldsFor(value);
        JPanel form = new JPanel(new GridBagLayout()); form.setOpaque(false);
        List<JTextField> inputs = new ArrayList<JTextField>();
        for (int index = 0; index < labels.length; index++) {
            GridBagConstraints label = new GridBagConstraints(); label.gridx = 0; label.gridy = index;
            label.anchor = GridBagConstraints.WEST; label.insets = new Insets(3, 0, 3, 8);
            form.add(UiFactory.body(labels[index]), label);
            JTextField field = UiFactory.textField(24); inputs.add(field);
            GridBagConstraints input = new GridBagConstraints(); input.gridx = 1; input.gridy = index;
            input.weightx = 1; input.fill = GridBagConstraints.HORIZONTAL; input.insets = new Insets(3, 0, 3, 0);
            form.add(field, input);
        }
        JButton proceed = new PrimaryButton("补充并继续代办");
        proceed.addActionListener(e -> {
            StringBuilder values = new StringBuilder();
            for (int index = 0; index < inputs.size(); index++) {
                String entered = inputs.get(index).getText().trim();
                if (entered.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "请填写“" + labels[index] + "”。"); return;
                }
                if (values.length() > 0) values.append("；");
                values.append(labels[index]).append("：").append(entered);
            }
            submit.accept(values.toString());
        });
        form.setAlignmentX(Component.LEFT_ALIGNMENT); proceed.setAlignmentX(Component.LEFT_ALIGNMENT);
        extras.add(Box.createVerticalStrut(6)); extras.add(form); extras.add(Box.createVerticalStrut(7));
        extras.add(proceed);
    }

    private void makePending(String value) {
        heading.setText("待确认业务操作");
        replaceBody(value.replace("【待确认操作】", "").trim());
        accent = DesignTokens.WARNING; setBackground(DesignTokens.WARNING_BACKGROUND); restyle();
    }

    private void addEvidence(List<AiAnswerEvidence> evidence) {
        if (evidence == null || evidence.isEmpty()) return;
        final JPanel detail = new JPanel(); detail.setOpaque(false);
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS)); detail.setVisible(false);
        for (AiAnswerEvidence item : evidence) {
            JTextArea row = UiFactory.textArea(2, 52); row.setEditable(false); row.setOpaque(false);
            row.setLineWrap(true); row.setWrapStyleWord(true); row.setBorder(
                    BorderFactory.createEmptyBorder(5, 8, 5, 8));
            row.setText(item.getTitle() + " · " + item.getSourceType() + " · "
                    + new Date(item.getUpdatedAt()) + "\n" + item.getExcerpt());
            row.setAlignmentX(Component.LEFT_ALIGNMENT); detail.add(row);
        }
        final JButton toggle = new SecondaryButton("回答依据（" + evidence.size() + "） ▸");
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(e -> {
            boolean show = !detail.isVisible(); detail.setVisible(show);
            toggle.setText("回答依据（" + evidence.size() + "） " + (show ? "▾" : "▸"));
            revalidate(); repaint();
        });
        extras.add(Box.createVerticalStrut(7)); extras.add(toggle); extras.add(detail);
    }

    private String[] fieldsFor(String value) {
        int marker = value.indexOf("待补充字段：");
        if (marker >= 0) {
            List<String> labels = new ArrayList<String>();
            String raw = value.substring(marker + "待补充字段：".length()).split("\\s", 2)[0];
            for (String key : raw.split(",")) labels.add(labelFor(key, value));
            if (!labels.isEmpty()) return labels.toArray(new String[labels.size()]);
        }
        if (value.contains("自习室")) return new String[]{"自习室编号", "开始时间", "结束时间"};
        if (value.contains("报修")) return new String[]{"房间编号", "故障类别", "故障描述"};
        if (value.contains("请假类型")) return new String[]{"请假类型", "开始时间", "结束时间", "原因"};
        if (value.contains("教室申请")) return new String[]{"教室编号", "用途", "开始时间", "结束时间"};
        if (value.contains("商品数量")) return new String[]{"商品数量"};
        if (value.contains("优惠券")) return new String[]{"优惠券代码"};
        if (value.contains("课程")) return new String[]{"课程名称或编号"};
        if (value.contains("哪本书") || value.contains("归还")) return new String[]{"书名或记录编号"};
        if (value.contains("竞赛")) return new String[]{"竞赛名称或编号"};
        if (value.contains("订单")) return new String[]{"订单编号"};
        if (value.contains("水电")) return new String[]{"水电分摊编号"};
        return new String[]{"需要补充的信息"};
    }

    private String labelFor(String key, String context) {
        if ("roomId".equals(key)) return context.contains("自习室") ? "自习室编号" : "房间编号";
        if ("startAt".equals(key)) return "开始时间";
        if ("endAt".equals(key)) return "结束时间";
        if ("category".equals(key)) return "故障类别";
        if ("description".equals(key)) return "故障描述";
        if ("leaveType".equals(key)) return "请假类型";
        if ("reason".equals(key)) return "原因";
        if ("classroomId".equals(key)) return "教室编号";
        if ("purpose".equals(key)) return "用途";
        if ("orderId".equals(key)) return "订单编号";
        if ("allocationId".equals(key)) return "水电分摊编号";
        if ("code".equals(key)) return "优惠券代码";
        return key;
    }

    private void replaceBody(String value) {
        text.setLength(0); text.append(value); body.setText(value); resizeBody();
    }

    private void resizeBody() {
        int lines = Math.max(1, Math.min(18, body.getLineCount() + getText().length() / 70));
        body.setRows(lines);
    }

    private void restyle() {
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
    }

    private void copy() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(getText()), null);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "当前环境无法访问剪贴板。");
        }
    }

    @Override public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }
}
