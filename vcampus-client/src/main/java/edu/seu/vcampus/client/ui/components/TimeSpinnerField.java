package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** 日期时间控件共用的时、分微型输入单元。 */
public final class TimeSpinnerField {
    private TimeSpinnerField() { }

    public static JPanel unit(JSpinner spinner, String suffix, int fontSize,
                              int width, int height) {
        spinner.setFont(DesignTokens.regular(fontSize));
        spinner.setPreferredSize(new Dimension(width, height));
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
}
