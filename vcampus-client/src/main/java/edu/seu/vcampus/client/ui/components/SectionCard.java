package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** 白色内容卡片，统一标题、说明和主体内边距。 */
public class SectionCard extends JPanel {
    private final JPanel body = new JPanel(new BorderLayout());

    public SectionCard(String title, String subtitle) {
        super(new BorderLayout(0, DesignTokens.SPACE_12));
        setOpaque(false);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(17, 17, 17, 17));

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        heading.setOpaque(false);
        JLabel titleLabel = UiFactory.sectionTitle(title);
        heading.add(titleLabel, BorderLayout.NORTH);
        if (subtitle != null && subtitle.trim().length() > 0) {
            heading.add(UiFactory.muted(subtitle), BorderLayout.SOUTH);
        }
        add(heading, BorderLayout.NORTH);

        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder());
        add(body, BorderLayout.CENTER);
    }

    public void setContent(JComponent content) {
        body.removeAll();
        body.add(content, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    public JPanel getBody() {
        return body;
    }

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                DesignTokens.RADIUS * 2, DesignTokens.RADIUS * 2);
        g.dispose();
        super.paintComponent(graphics);
    }

    @Override protected void paintBorder(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(DesignTokens.BORDER_LIGHT);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                DesignTokens.RADIUS * 2, DesignTokens.RADIUS * 2);
        g.dispose();
    }
}
