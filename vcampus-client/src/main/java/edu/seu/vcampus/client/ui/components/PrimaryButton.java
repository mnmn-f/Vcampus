package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.Graphics;
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
        setContentAreaFilled(false);
        setOpaque(false);
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

    /**
     * 圆角填充。
     *
     * <p>原来靠 {@code setOpaque(true)} 让 Swing 填一个直角矩形，所以按钮是方的，
     * 和圆角的卡片、输入框放在一起很突兀。这里关掉默认填充自己画圆角，顺便在底部
     * 压一道更深的边，让按钮看起来是「按得下去」的实体而不是一块色板。</p>
     */
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = DesignTokens.RADIUS;
        int h = getHeight();
        Color fill = isEnabled() ? getBackground() : DesignTokens.BORDER;
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth(), h, arc, arc);
        if (isEnabled()) {
            g.setColor(DesignTokens.PRIMARY_PRESSED);
            g.drawRoundRect(0, 1, getWidth() - 1, h - 2, arc, arc);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
