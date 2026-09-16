package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.GridLayout;

/** 公告使用的“日期 + 时间”双下拉框，日期可选择不设置。 */
final class DateTimeDropdown extends JPanel {
    private static final String EMPTY = "不设置";
    private static final int DATE_RANGE_DAYS = 365;

    private final JComboBox<String> date = new JComboBox<String>();
    private final TimeDropdown time = new TimeDropdown();

    DateTimeDropdown() {
        super(new GridLayout(1, 2, 6, 0));
        setOpaque(false);
        date.setFont(DesignTokens.regular(13));
        date.addItem(EMPTY);
        LocalDate today = LocalDate.now();
        for (int i = 0; i <= DATE_RANGE_DAYS; i++) date.addItem(today.plusDays(i).toString());
        time.setTime(LocalTime.of(8, 0));
        time.setEnabled(false);
        date.addActionListener(event -> time.setEnabled(date.getSelectedIndex() > 0));
        add(date);
        add(time);
    }

    LocalDateTime getDateTime() {
        if (date.getSelectedIndex() <= 0) return null;
        return LocalDate.parse(String.valueOf(date.getSelectedItem())).atTime(time.getTime());
    }

    void setDateTime(LocalDateTime value) {
        if (value == null) {
            date.setSelectedIndex(0);
            return;
        }
        String day = value.toLocalDate().toString();
        if (!containsDate(day)) date.addItem(day);
        date.setSelectedItem(day);
        time.setTime(value.toLocalTime());
        time.setEnabled(true);
    }

    private boolean containsDate(String value) {
        for (int i = 0; i < date.getItemCount(); i++) {
            if (value.equals(date.getItemAt(i))) return true;
        }
        return false;
    }
}
