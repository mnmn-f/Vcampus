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
    private final JTextArea body = UiFactory.textArea(1, 10);
    private final JPanel extras = new JPanel();
    private final List<AbstractButton> feedbackButtons = new ArrayList<AbstractButton>();
    private final StringBuilder text = new StringBuilder();
    private Color accent = DesignTokens.PRIMARY_BORDER;
    private Color bubble;

    AiMessageCard(Kind kind, String initial) {
        super(new BorderLayout(0, 7));
        bubble = kind == Kind.USER ? new Color(0xE8, 0xF3, 0xE5)
                : kind == Kind.SYSTEM ? new Color(0xF4, 0xF5, 0xF2) : Color.WHITE;
        setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        setOpaque(false);
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

    void addFeedbackActions(Runnable positive, Runnable negative, Runnable correction) {
        if (positive == null || negative == null || correction == null || !feedbackButtons.isEmpty()) return;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        actions.setOpaque(false);
        actions.add(UiFactory.muted("评价这条回复"));
        // Windows 的常用中文字体不一定包含彩色 emoji，旧标签会退化成方框。
        // 使用明确的中文文字可跨外观、跨系统稳定显示，也更利于键盘和读屏识别。
        feedbackButtons.add(feedbackButton("点赞", "这条回复有帮助", positive, actions));
        feedbackButtons.add(feedbackButton("点踩", "这条回复需要改进", negative, actions));
        feedbackButtons.add(feedbackButton("纠错", "纠错", correction, actions));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        extras.add(Box.createVerticalStrut(7));
        extras.add(actions);
        revalidate(); repaint();
    }

    void markFeedbackSubmitted() {
        for (AbstractButton button : feedbackButtons) button.setEnabled(false);
    }

    private AbstractButton feedbackButton(String text, String tooltip, Runnable action,
                                          JPanel parent) {
        JButton button = new SecondaryButton(text);
        button.setToolTipText(tooltip);
        button.setMargin(new Insets(2, 8, 2, 8));
        button.addActionListener(e -> action.run());
        parent.add(button);
        return button;
    }

    private void makeBusiness(String value) {
        int close = value.indexOf('】');
        if (close > 0) {
            heading.setText("业务结果 · " + value.substring("【实时数据｜".length(), close));
            replaceBody(value.substring(close + 1).trim());
        } else heading.setText("业务结果");
        accent = DesignTokens.GOLD; bubble = new Color(0xFF, 0xFC, 0xF3); restyle();
    }

    private void makeParameters(String value, Consumer<String> submit) {
        heading.setText("待补充参数");
        replaceBody(value.replace("【还需要一点信息】", "").trim());
        accent = DesignTokens.WARNING; bubble = DesignTokens.WARNING_BACKGROUND; restyle();
        List<FieldSpec> fields = fieldsFor(value);
        JPanel form = new JPanel(new GridBagLayout()); form.setOpaque(false);
        List<FieldInput> inputs = new ArrayList<FieldInput>();
        for (int index = 0; index < fields.size(); index++) {
            FieldSpec spec = fields.get(index);
            GridBagConstraints label = new GridBagConstraints(); label.gridx = 0; label.gridy = index;
            label.anchor = GridBagConstraints.WEST; label.insets = new Insets(3, 0, 3, 8);
            form.add(UiFactory.body(spec.label), label);
            FieldInput field = inputFor(spec); inputs.add(field);
            GridBagConstraints input = new GridBagConstraints(); input.gridx = 1; input.gridy = index;
            input.weightx = 1; input.fill = GridBagConstraints.HORIZONTAL; input.insets = new Insets(3, 0, 3, 0);
            form.add(field.component, input);
        }
        JButton proceed = new PrimaryButton("补充并继续代办");
        proceed.addActionListener(e -> {
            StringBuilder values = new StringBuilder();
            for (int index = 0; index < inputs.size(); index++) {
                String entered = inputs.get(index).value().trim();
                if (entered.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "请填写“" + fields.get(index).label + "”。"); return;
                }
                if (values.length() > 0) values.append("；");
                values.append(fields.get(index).label).append("：").append(entered);
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
        accent = DesignTokens.WARNING; bubble = DesignTokens.WARNING_BACKGROUND; restyle();
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

    private List<FieldSpec> fieldsFor(String value) {
        List<FieldSpec> fields = new ArrayList<FieldSpec>();
        int marker = value.indexOf("待补充字段：");
        if (marker >= 0) {
            String raw = value.substring(marker + "待补充字段：".length()).split("[\\r\\n]", 2)[0];
            for (String key : raw.split("[,，]")) {
                String clean = key.replaceAll("[^A-Za-z0-9_]", "").trim();
                if (!clean.isEmpty()) addField(fields, clean, value);
            }
        }
        // 自习室即使被模型自动猜中，也保留房间编号让用户明确选择。
        if (value.contains("自习室")) {
            addFieldAtStart(fields, "roomId", value);
            addField(fields, "startAt", value); addField(fields, "endAt", value);
        }
        if (fields.isEmpty() && value.contains("报修")) {
            addField(fields, "roomId", value); addField(fields, "category", value);
            addField(fields, "description", value);
        }
        if (fields.isEmpty() && value.contains("请假类型")) {
            addField(fields, "leaveType", value); addField(fields, "startAt", value);
            addField(fields, "endAt", value); addField(fields, "reason", value);
        }
        if (fields.isEmpty() && value.contains("教室申请")) {
            addField(fields, "classroomId", value); addField(fields, "purpose", value);
            addField(fields, "startAt", value); addField(fields, "endAt", value);
        }
        if (fields.isEmpty()) {
            if (value.contains("商品数量")) addField(fields, "quantity", value);
            else if (value.contains("优惠券")) addField(fields, "code", value);
            else if (value.contains("课程")) addField(fields, "course", value);
            else if (value.contains("哪本书") || value.contains("归还")) addField(fields, "book", value);
            else if (value.contains("竞赛")) addField(fields, "competition", value);
            else if (value.contains("订单")) addField(fields, "orderId", value);
            else if (value.contains("水电")) addField(fields, "allocationId", value);
            else addField(fields, "details", value);
        }
        return fields;
    }

    private void addField(List<FieldSpec> fields, String key, String context) {
        for (FieldSpec field : fields) if (field.key.equals(key)) return;
        fields.add(new FieldSpec(key, labelFor(key, context)));
    }

    private void addFieldAtStart(List<FieldSpec> fields, String key, String context) {
        for (FieldSpec field : fields) if (field.key.equals(key)) return;
        fields.add(0, new FieldSpec(key, labelFor(key, context)));
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
        if ("quantity".equals(key)) return "商品数量";
        if ("course".equals(key)) return "课程名称或编号";
        if ("book".equals(key)) return "书名或记录编号";
        if ("competition".equals(key)) return "竞赛名称或编号";
        if ("details".equals(key)) return "需要补充的信息";
        return key;
    }

    private FieldInput inputFor(FieldSpec spec) {
        if ("startAt".equals(spec.key) || "endAt".equals(spec.key)) {
            final AiDateTimeField dateTime = new AiDateTimeField("endAt".equals(spec.key));
            return new FieldInput(dateTime) { String value() { return dateTime.value(); }};
        }
        final JTextField textField = UiFactory.textField(24);
        return new FieldInput(textField) { String value() { return textField.getText(); }};
    }

    private void replaceBody(String value) {
        text.setLength(0); text.append(value); body.setText(value); resizeBody();
    }

    private void resizeBody() {
        String[] logical = getText().split("\\r?\\n", -1);
        int longest = 0;
        for (String line : logical) longest = Math.max(longest, line.length());
        int columns = Math.max(10, Math.min(44, longest + 1));
        int rows = 0;
        for (String line : logical) rows += Math.max(1,
                (line.length() + columns - 1) / columns);
        body.setColumns(columns);
        body.setRows(Math.max(1, rows));
    }

    private void restyle() {
        setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
    }

    private void copy() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(getText()), null);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "当前环境无法访问剪贴板。");
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(bubble);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        g.setColor(accent);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        g.dispose();
        super.paintComponent(graphics);
    }

    private static final class FieldSpec {
        private final String key;
        private final String label;
        private FieldSpec(String key, String label) { this.key = key; this.label = label; }
    }

    private abstract static class FieldInput {
        private final JComponent component;
        private FieldInput(JComponent component) { this.component = component; }
        abstract String value();
    }
}
