package edu.seu.vcampus.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Swing 控件的集中构造与通用样式。 */
public final class UiFactory {
    private UiFactory() {
    }

    /**
     * 安装外观并统一字体、表格和标签页的基础样式。
     *
     * <p>用 FlatLaf 替换系统外观：系统外观在 Windows 上是 Windows 10 时代的控件，
     * 直角、焦点框粗、高分屏下字体发虚。FlatLaf 只换控件本身的绘制（圆角、间距、
     * 焦点环、滚动条、HiDPI 缩放），下面那些 DesignTokens 颜色常量一个都不受影响，
     * 所以主色、卡片和页面配色与原来完全一致。</p>
     *
     * <p>装不上时静默退回系统外观——外观是锦上添花，不该因为它启动不了客户端。</p>
     */
    public static void configureLookAndFeel() {
        // 强调色交给 FlatLaf，让复选框、单选框、进度条、文本选中这些由外观自己
        // 绘制的控件也用上主色，而不是各平台默认的蓝。必须在 setup() 之前设置，
        // 因为 FlatLaf 是在安装外观时一次性解析这些默认值的。
        com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(
                java.util.Collections.singletonMap("@accentColor", hex(DesignTokens.PRIMARY)));
        if (!com.formdev.flatlaf.FlatLightLaf.setup()) {
            try {
                javax.swing.UIManager.setLookAndFeel(
                        javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 使用 Swing 默认外观即可继续运行。
            }
        }
        // FlatLaf 的默认圆角偏小，配合本项目的卡片式布局调大一档。
        javax.swing.UIManager.put("Button.arc", Integer.valueOf(8));
        javax.swing.UIManager.put("Component.arc", Integer.valueOf(8));
        javax.swing.UIManager.put("TextComponent.arc", Integer.valueOf(6));
        javax.swing.UIManager.put("ScrollBar.thumbArc", Integer.valueOf(8));
        javax.swing.UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        javax.swing.UIManager.put("Component.focusColor", DesignTokens.PRIMARY_BORDER);
        javax.swing.UIManager.put("Component.focusedBorderColor", DesignTokens.PRIMARY);
        javax.swing.UIManager.put("Component.borderColor", DesignTokens.BORDER);
        javax.swing.UIManager.put("TitlePane.unifiedBackground", Boolean.FALSE);
        // 正文整体大一号：14 在 1440 宽的窗口上偏小，行距一挤就糊成一片。
        javax.swing.UIManager.put("Label.font", DesignTokens.regular(15));
        javax.swing.UIManager.put("Button.font", DesignTokens.regular(14));
        javax.swing.UIManager.put("TextField.font", DesignTokens.regular(15));
        javax.swing.UIManager.put("PasswordField.font", DesignTokens.regular(15));
        javax.swing.UIManager.put("FormattedTextField.font", DesignTokens.regular(15));
        javax.swing.UIManager.put("TextArea.font", DesignTokens.regular(15));
        javax.swing.UIManager.put("ComboBox.font", DesignTokens.regular(15));
        javax.swing.UIManager.put("Spinner.font", DesignTokens.regular(15));
        javax.swing.UIManager.put("List.font", DesignTokens.regular(14));
        javax.swing.UIManager.put("TabbedPane.font", DesignTokens.regular(14));
        javax.swing.UIManager.put("Table.font", DesignTokens.regular(14));
        // 表头退成「12 号灰字压在页面底色上」：它是列的说明，不是一条独立的灰带。
        javax.swing.UIManager.put("TableHeader.font", DesignTokens.medium(12));
        javax.swing.UIManager.put("Table.rowHeight", 35);
        javax.swing.UIManager.put("Table.showGrid", Boolean.FALSE);
        javax.swing.UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        javax.swing.UIManager.put("Table.gridColor", DesignTokens.BORDER_LIGHT);
        javax.swing.UIManager.put("Table.selectionBackground", DesignTokens.PRIMARY_LIGHT);
        javax.swing.UIManager.put("Table.selectionForeground", DesignTokens.TEXT_PRIMARY);
        javax.swing.UIManager.put("TableHeader.background", DesignTokens.PAGE_BACKGROUND);
        javax.swing.UIManager.put("TableHeader.foreground", DesignTokens.TEXT_SECONDARY);
        javax.swing.UIManager.put("TableHeader.separatorColor", DesignTokens.BORDER);
        javax.swing.UIManager.put("TableHeader.bottomSeparatorColor", DesignTokens.BORDER);
        javax.swing.UIManager.put("TabbedPane.selected", Color.WHITE);
        javax.swing.UIManager.put("TabbedPane.background", DesignTokens.PAGE_BACKGROUND);
        javax.swing.UIManager.put("TabbedPane.focus", DesignTokens.PRIMARY_BORDER);
    }

    /** FlatLaf 的 @accentColor 只认 #RRGGBB 字符串，这里把颜色常量转过去。 */
    private static String hex(Color color) {
        return String.format("#%02X%02X%02X", Integer.valueOf(color.getRed()),
                Integer.valueOf(color.getGreen()), Integer.valueOf(color.getBlue()));
    }

    public static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DesignTokens.PAGE_BACKGROUND);
        return panel;
    }

    public static JPanel horizontal(int gap) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel vertical(int gap) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.putClientProperty("vcampus.gap", Integer.valueOf(gap));
        return panel;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignTokens.medium(24));
        label.setForeground(DesignTokens.TEXT_PRIMARY);
        return label;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignTokens.medium(17));
        label.setForeground(DesignTokens.TEXT_PRIMARY);
        return label;
    }

    public static JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignTokens.regular(15));
        label.setForeground(DesignTokens.TEXT_PRIMARY);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignTokens.regular(12));
        label.setForeground(DesignTokens.TEXT_SECONDARY);
        return label;
    }

    public static JLabel requiredLabel(String text) {
        JLabel label = body(text + " *");
        label.setForeground(DesignTokens.TEXT_PRIMARY);
        return label;
    }

    public static JTextField textField(int columns) {
        JTextField field = new JTextField(columns);
        styleField(field);
        return field;
    }

    public static JTextArea textArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(DesignTokens.regular(15));
        area.setForeground(DesignTokens.TEXT_PRIMARY);
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesignTokens.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return area;
    }

    public static void styleField(JTextField field) {
        field.setFont(DesignTokens.regular(15));
        field.setForeground(DesignTokens.TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
        field.setCaretColor(DesignTokens.PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesignTokens.BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 36));
    }

    /** 登录页使用的横向输入线，避免在大面积留白里出现厚重的方框。 */
    public static void styleLoginField(JTextField field) {
        field.setFont(DesignTokens.regular(16));
        field.setForeground(DesignTokens.TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
        field.setCaretColor(DesignTokens.PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER),
                BorderFactory.createEmptyBorder(9, 2, 7, 2)));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 42));
    }

    public static JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        button.setMargin(DesignTokens.NO_INSETS);
        button.setFont(DesignTokens.regular(13));
        button.setForeground(DesignTokens.PRIMARY);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        return button;
    }

    public static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.insets = new Insets(0, 0, DesignTokens.SPACE_12, DesignTokens.SPACE_12);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        return constraints;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesignTokens.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(DesignTokens.SPACE_16, DesignTokens.SPACE_16,
                        DesignTokens.SPACE_16, DesignTokens.SPACE_16));
    }

    public static JPanel twoColumnForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel labelledField(String label, java.awt.Component field) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.add(body(label), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }
}
