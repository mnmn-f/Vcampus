package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;

import javax.swing.JComboBox;

/** 以半小时为步长的时间下拉框；仍可完整显示已有的非标准分钟值。 */
final class TimeDropdown extends JComboBox<String> {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    TimeDropdown() {
        for (int hour = 0; hour < 24; hour++) {
            addItem(String.format("%02d:00", hour));
            addItem(String.format("%02d:30", hour));
        }
        setFont(DesignTokens.regular(13));
    }

    LocalTime getTime() {
        Object selected = getSelectedItem();
        if (selected == null) throw new IllegalArgumentException("请选择时间");
        return LocalTime.parse(String.valueOf(selected), FORMAT);
    }

    void setTime(LocalTime value) {
        if (value == null) return;
        String text = FORMAT.format(value);
        if (!contains(text)) addItem(text);
        setSelectedItem(text);
    }

    private boolean contains(String value) {
        for (int i = 0; i < getItemCount(); i++) {
            if (value.equals(getItemAt(i))) return true;
        }
        return false;
    }
}
