package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 无额外依赖的日期选择器；保留标准日期文本，禁用过去日期。 */
public final class CalendarDateField extends JPanel {
    private final JTextField value = UiFactory.textField(10);
    private final JButton open = new SecondaryButton("日历");
    private YearMonth month;
    private final JPopupMenu popup = new JPopupMenu();

    public CalendarDateField() {
        super(new BorderLayout(6, 0)); setOpaque(false);
        value.setEditable(false); value.setText(LocalDate.now().plusDays(1).toString());
        value.setToolTipText("点击日历选择预约日期");
        add(value, BorderLayout.CENTER); add(open, BorderLayout.EAST);
        open.addActionListener(event -> {
            month = YearMonth.from(getDate()); renderMonth(); popup.show(open, 0, open.getHeight());
        });
    }
    public LocalDate getDate() { return LocalDate.parse(value.getText()); }
    public void setDate(LocalDate date) { value.setText(date.toString()); }
    private void renderMonth() {
        popup.removeAll(); JPanel content = new JPanel(new BorderLayout(6, 6));
        JPanel navigation = new JPanel(new BorderLayout());
        JButton previous = new SecondaryButton("‹"); JButton next = new SecondaryButton("›");
        navigation.add(previous, BorderLayout.WEST); navigation.add(new JLabel(month.toString(), JLabel.CENTER), BorderLayout.CENTER);
        navigation.add(next, BorderLayout.EAST); content.add(navigation, BorderLayout.NORTH);
        previous.addActionListener(event -> { month = month.minusMonths(1); renderMonth(); });
        next.addActionListener(event -> { month = month.plusMonths(1); renderMonth(); });
        JPanel days = new JPanel(new GridLayout(0, 7, 3, 3));
        for (String day : new String[]{"一", "二", "三", "四", "五", "六", "日"}) days.add(new JLabel(day, JLabel.CENTER));
        for (int i = 1; i < month.atDay(1).getDayOfWeek().getValue(); i++) days.add(new JLabel());
        for (int i = 1; i <= month.lengthOfMonth(); i++) {
            final LocalDate day = month.atDay(i); JButton button = new JButton(String.valueOf(i));
            button.setEnabled(!day.isBefore(LocalDate.now()));
            if (day.equals(getDate())) button.setForeground(edu.seu.vcampus.client.ui.DesignTokens.PRIMARY);
            button.addActionListener(event -> { setDate(day); popup.setVisible(false); }); days.add(button);
        }
        content.add(days, BorderLayout.CENTER); popup.add(content); popup.pack();
    }
}
