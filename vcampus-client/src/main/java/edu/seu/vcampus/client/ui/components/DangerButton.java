package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;

/** 高风险操作按钮。调用方必须搭配确认步骤。 */
public class DangerButton extends JButton {
    public DangerButton(String text) {
        super(text);
        setUI(new BasicButtonUI());
        setFont(DesignTokens.regular(14));
        setForeground(DesignTokens.ERROR);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE8, 0xA0, 0x9A)),
                BorderFactory.createEmptyBorder(7, 15, 7, 15)));
        setFocusPainted(false);
        setContentAreaFilled(true);
        setOpaque(true);
    }
}
