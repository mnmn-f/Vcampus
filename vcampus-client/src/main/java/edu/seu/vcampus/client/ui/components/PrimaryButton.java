package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 主要操作按钮，表达页面最重要的确定性动作。 */
public class PrimaryButton extends JButton {
    private final Color normal = DesignTokens.PRIMARY;

    public PrimaryButton(String text) {
        super(text);
        setUI(new BasicButtonUI());
        setFont(DesignTokens.regular(14));
        setForeground(Color.WHITE);
        setBackground(normal);
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        setFocusPainted(false);
        setContentAreaFilled(true);
        setOpaque(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int textWidth = getFontMetrics(getFont()).stringWidth(text == null ? "" : text) + 32;
        Dimension size = new Dimension(Math.max(DesignTokens.BUTTON_SIZE.width, textWidth),
                DesignTokens.BUTTON_SIZE.height);
        setMinimumSize(size);
        setPreferredSize(size);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(DesignTokens.PRIMARY_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(normal);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(DesignTokens.PRIMARY_PRESSED);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    setBackground(normal);
                }
            }
        });
    }
}
