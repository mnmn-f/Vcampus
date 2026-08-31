package edu.seu.vcampus.client.ui.components;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/** 将一个业务模块拆成用户可识别的二级任务页。 */
public final class TaskTabs extends JTabbedPane {
    public TaskTabs() {
        super(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        setFont(DesignTokens.medium(14));
        setUI(new TaskTabUi());
        setForeground(DesignTokens.TEXT_PRIMARY);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(DesignTokens.BORDER_LIGHT));
        setMinimumSize(new Dimension(0, 0));
    }

    public TaskTabs addTask(String title, JComponent content) {
        JPanel canvas = new JPanel(new BorderLayout());
        canvas.setBackground(DesignTokens.PAGE_BACKGROUND);
        canvas.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        canvas.setMinimumSize(new Dimension(0, 0));
        content.setMinimumSize(new Dimension(0, 0));
        canvas.add(content, BorderLayout.NORTH);
        addTab(title, canvas);
        return this;
    }

    public TaskTabs addTask(String title, JComponent first, JComponent... rest) {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        add(stack, first);
        if (rest != null) for (JComponent item : rest) add(stack, item);
        return addTask(title, stack);
    }

    private void add(JPanel stack, JComponent item) {
        if (item == null) return;
        item.setAlignmentX(LEFT_ALIGNMENT);
        stack.add(item);
        stack.add(Box.createVerticalStrut(14));
    }

    private static final class TaskTabUi extends BasicTabbedPaneUI {
        @Override protected Insets getTabInsets(int placement, int index) {
            return new Insets(10, 18, 10, 18);
        }

        @Override protected void paintTabBackground(Graphics g, int placement, int index,
                int x, int y, int width, int height, boolean selected) {
            g.setColor(selected ? DesignTokens.PRIMARY : DesignTokens.PAGE_BACKGROUND);
            g.fillRect(x, y, width, height);
        }

        @Override protected void paintText(Graphics g, int placement, Font font,
                FontMetrics metrics, int index, String title, Rectangle bounds,
                boolean selected) {
            g.setFont(font);
            g.setColor(selected ? Color.WHITE : DesignTokens.TEXT_SECONDARY);
            int x = bounds.x + (bounds.width - metrics.stringWidth(title)) / 2;
            int y = bounds.y + (bounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(title, x, y);
        }

        @Override protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects,
                int index, Rectangle icon, Rectangle text, boolean selected) { }

        @Override protected void paintContentBorder(Graphics g, int placement, int selected) { }
    }
}
