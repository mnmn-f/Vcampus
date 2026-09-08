package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.view.modules.real.CalendarDateField;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** AI 代办卡片使用的“日历 + 时分”选择器。 */
final class AiDateTimeField extends JPanel {
    private static final int MINUTE_STEP = 5;

    private final CalendarDateField date = new CalendarDateField();
    private final JSpinner hour;
    private final JSpinner minute = new JSpinner(new SpinnerNumberModel(0, 0, 55, MINUTE_STEP));

    AiDateTimeField(boolean endTime) {
        super(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setOpaque(false);
        hour = new JSpinner(new SpinnerNumberModel(endTime ? 16 : 14, 0, 23, 1));
        add(date);
        add(unit(hour, "时"));
        add(unit(minute, "分"));
    }

    private JPanel unit(JSpinner spinner, String suffix) {
        spinner.setFont(DesignTokens.regular(14));
        spinner.setPreferredSize(new Dimension(56, 34));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "00");
        editor.getTextField().setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        spinner.setEditor(editor);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        row.setOpaque(false);
        row.add(spinner);
        JLabel label = new JLabel(suffix);
        label.setFont(DesignTokens.regular(13));
        label.setForeground(DesignTokens.TEXT_SECONDARY);
        row.add(label);
        return row;
    }

    String value() {
        LocalDate day = date.getDate();
        if (day == null) return "";
        LocalDateTime value = day.atTime(((Number) hour.getValue()).intValue(),
                ((Number) minute.getValue()).intValue());
        return value.toString();
    }
}
