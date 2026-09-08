package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;

/**
 * 日期输入：点一下弹日历选，不用手打。
 *
 * <p>原来是让人往文本框里敲 {@code yyyy-MM-dd HH:mm}，敲错一个字符就被打回来重填，
 * 而且没人记得月份要不要补零。日历面板把「哪天」这件事变回它本来的样子——看着
 * 月历点一下。</p>
 *
 * <p>输入框保持只读：能选就不该能敲，否则又要把手输格式的那套校验养回来。清空和
 * 「今天」这两个动作在弹出的日历面板里。</p>
 */
public final class DormDateField extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String[] WEEKDAYS = {"一", "二", "三", "四", "五", "六", "日"};

    private final JTextField display = new JTextField();
    private final JPopupMenu popup = new JPopupMenu();
    private final JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private YearMonth shown = YearMonth.now();
    private LocalDate value;

    public DormDateField(int columns) {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        display.setColumns(columns);
        display.setEditable(false);
        display.setFont(DesignTokens.regular(15));
        display.setForeground(DesignTokens.TEXT_PRIMARY);
        display.setBackground(Color.WHITE);
        display.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        display.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesignTokens.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        display.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { open(); }
        });
        add(display, BorderLayout.CENTER);
        // 没有额外的「日历」按钮：整个输入框本身就是那个按钮，再放一个只会让人犹豫
        // 该点哪个，而两个的行为完全一样。
        buildPopup();
        setText();
    }

    public LocalDate getDate() { return value; }

    public void setDate(LocalDate date) {
        value = date;
        if (date != null) shown = YearMonth.of(date.getYear(), date.getMonthValue());
        setText();
    }

    public void clear() { setDate(null); }

    /** 取值并在为空时报错，错误话术和其他必填项一致。 */
    public LocalDate required(String label) {
        if (value == null) throw new IllegalArgumentException("请选择" + label);
        return value;
    }

    private void setText() {
        display.setText(value == null ? "" : value.toString());
        display.setForeground(value == null ? DesignTokens.TEXT_PLACEHOLDER : DesignTokens.TEXT_PRIMARY);
        if (value == null) display.setText("点击选择日期");
    }

    private void open() {
        if (popup.isVisible()) { popup.setVisible(false); return; }
        shown = value == null ? YearMonth.now() : YearMonth.of(value.getYear(), value.getMonthValue());
        renderMonth();
        popup.show(display, 0, display.getHeight());
    }

    private void buildPopup() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(true);
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(navButton("‹", -1), BorderLayout.WEST);
        monthLabel.setFont(DesignTokens.medium(15));
        monthLabel.setForeground(DesignTokens.TEXT_PRIMARY);
        head.add(monthLabel, BorderLayout.CENTER);
        head.add(navButton("›", 1), BorderLayout.EAST);
        body.add(head, BorderLayout.NORTH);

        JPanel calendar = new JPanel(new BorderLayout(0, 4));
        calendar.setOpaque(false);
        JPanel weekdays = new JPanel(new GridLayout(1, 7, 2, 2));
        weekdays.setOpaque(false);
        for (String name : WEEKDAYS) {
            JLabel label = new JLabel(name, SwingConstants.CENTER);
            label.setFont(DesignTokens.regular(12));
            label.setForeground(DesignTokens.TEXT_SECONDARY);
            weekdays.add(label);
        }
        calendar.add(weekdays, BorderLayout.NORTH);
        grid.setOpaque(false);
        calendar.add(grid, BorderLayout.CENTER);
        body.add(calendar, BorderLayout.CENTER);

        JPanel foot = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        foot.setOpaque(false);
        foot.add(textButton("今天", new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { pick(LocalDate.now()); }
        }));
        foot.add(textButton("清除", new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { pick(null); }
        }));
        body.add(foot, BorderLayout.SOUTH);

        popup.setBorder(BorderFactory.createLineBorder(DesignTokens.BORDER));
        popup.add(body);
    }

    private JButton navButton(String text, final int months) {
        JButton button = textButton(text, null);
        button.setFont(DesignTokens.medium(16));
        button.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                shown = shown.plusMonths(months);
                renderMonth();
            }
        });
        return button;
    }

    private JButton textButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(DesignTokens.regular(13));
        button.setForeground(DesignTokens.PRIMARY);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (listener != null) button.addActionListener(listener);
        return button;
    }

    private void renderMonth() {
        monthLabel.setText(shown.getYear() + " 年 " + shown.getMonthValue() + " 月");
        grid.removeAll();
        LocalDate first = shown.atDay(1);
        // ISO 里周一是 1；日历第一行要空出这个月 1 号之前的格子。
        int leading = first.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leading; i++) grid.add(blank());
        int days = shown.lengthOfMonth();
        for (int day = 1; day <= days; day++) grid.add(dayButton(shown.atDay(day)));
        // 补满到整行，否则最后一行的格子会被拉高。
        while (grid.getComponentCount() % 7 != 0) grid.add(blank());
        grid.revalidate();
        grid.repaint();
        popup.pack();
    }

    private JPanel blank() {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setPreferredSize(new Dimension(34, 30));
        return cell;
    }

    private JButton dayButton(final LocalDate date) {
        JButton button = new JButton(String.valueOf(date.getDayOfMonth()));
        boolean selected = date.equals(value);
        boolean today = date.equals(LocalDate.now());
        button.setFont(selected ? DesignTokens.medium(13) : DesignTokens.regular(13));
        button.setPreferredSize(new Dimension(34, 30));
        button.setFocusPainted(false);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(selected);
        button.setOpaque(selected);
        button.setBackground(selected ? DesignTokens.PRIMARY : Color.WHITE);
        button.setForeground(selected ? Color.WHITE
                : today ? DesignTokens.PRIMARY : DesignTokens.TEXT_PRIMARY);
        button.setBorder(today && !selected
                ? BorderFactory.createLineBorder(DesignTokens.PRIMARY_BORDER)
                : BorderFactory.createEmptyBorder(1, 1, 1, 1));
        button.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { pick(date); }
        });
        return button;
    }

    private void pick(LocalDate date) {
        setDate(date);
        popup.setVisible(false);
    }
}
