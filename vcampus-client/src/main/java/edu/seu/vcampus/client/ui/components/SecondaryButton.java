package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 次要操作按钮，避免和页面主动作争夺注意力。 */
public class SecondaryButton extends JButton {
    public SecondaryButton(String text) {
        super(text);
        setUI(new BasicButtonUI());
        setFont(DesignTokens.regular(14));
        setForeground(DesignTokens.TEXT_PRIMARY);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesignTokens.BORDER),
                BorderFactory.createEmptyBorder(7, 15, 7, 15)));
        setFocusPainted(false);
        setContentAreaFilled(true);
        setOpaque(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(DesignTokens.PRIMARY_LIGHT);
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(DesignTokens.PRIMARY_BORDER),
                            BorderFactory.createEmptyBorder(7, 15, 7, 15)));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(DesignTokens.BORDER),
                        BorderFactory.createEmptyBorder(7, 15, 7, 15)));
            }
        });
    }
}
