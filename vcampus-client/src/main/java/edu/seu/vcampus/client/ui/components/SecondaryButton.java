package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.Graphics;
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
        setContentAreaFilled(false);
        setOpaque(false);
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

    /**
     * 圆角描边。
     *
     * <p>与 {@link PrimaryButton} 同样的理由：默认填充画的是直角矩形。次要按钮只
     * 描边不压深色底边，保持它在视觉层级上低于主按钮。</p>
     */
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = DesignTokens.RADIUS;
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g.setColor(isEnabled() ? DesignTokens.PRIMARY_BORDER : DesignTokens.BORDER);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g.dispose();
        super.paintComponent(graphics);
    }
}
