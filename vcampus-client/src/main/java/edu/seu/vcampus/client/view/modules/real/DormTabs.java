package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.components.TaskTabs;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * 宿舍模块的标签栏。
 *
 * <p>{@link TaskTabs} 那套「衬线字 + 底部一道细指示条」在别的模块里还用着，这里只
 * 换宿舍这一份：选中项做成和左侧导航一样的实心圆角块。同一个人在同一屏上看到的两处
 * 「我在哪儿」，没有理由长成两种样子——左边是绿底白字，上面却是灰字加一道下划线，
 * 眼睛得学两遍。</p>
 *
 * <p>换成圆角块之后不再需要那条基线：块本身已经把选中项从一排文字里分出来了，再加
 * 一条横线就是两套指示同时开着。未选中项保持成页面底色上的普通文字，鼠标移上去才
 * 浅浅垫一层，避免整排看起来像一行按钮。</p>
 */
public final class DormTabs {
    private DormTabs() { }

    /**
     * 造一条宿舍模块的标签栏。
     *
     * <p>做成工厂而不是子类：{@link TaskTabs} 是 final 的，而它之所以 final 是有道理的
     * ——别的模块都直接用它那套默认样式，不该被谁继承一下就改了行为。这里换的只是外观，
     * 换外观本来就是 {@code setUI} 的事。</p>
     */
    public static TaskTabs create() {
        TaskTabs tabs = new TaskTabs();
        // 标签是导航，不是正文，用界面主字体即可；衬线只在 TaskTabs 的默认样式里用。
        tabs.setFont(DesignTokens.regular(14));
        tabs.setUI(new PillTabUi());
        return tabs;
    }

    /** 选中项画成实心圆角块，与左侧导航的选中态一致。 */
    private static final class PillTabUi extends BasicTabbedPaneUI {
        /** 圆角块之间留出的空隙，靠左右各缩进一半实现。 */
        private static final int GAP = 4;
        private static final int RADIUS = 9;

        @Override protected Insets getTabInsets(int placement, int index) {
            return new Insets(10, 22, 10, 22);
        }

        @Override protected Insets getTabAreaInsets(int placement) {
            return new Insets(0, 0, 6, 0);
        }

        @Override protected void paintTabBackground(Graphics graphics, int placement, int index,
                int x, int y, int width, int height, boolean selected) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(DesignTokens.PAGE_BACKGROUND);
            g.fillRect(x, y, width, height);
            boolean hovered = !selected && getRolloverTab() == index;
            if (selected || hovered) {
                g.setColor(selected ? DesignTokens.PRIMARY : DesignTokens.PRIMARY_LIGHT);
                g.fillRoundRect(x + GAP, y, width - GAP * 2, height, RADIUS, RADIUS);
            }
            g.dispose();
        }

        @Override protected void paintTabBorder(Graphics g, int placement, int index,
                int x, int y, int width, int height, boolean selected) { }

        @Override protected void paintText(Graphics g, int placement, Font font,
                FontMetrics metrics, int index, String title, Rectangle bounds, boolean selected) {
            Font actual = selected ? DesignTokens.medium(font.getSize()) : font;
            FontMetrics actualMetrics = g.getFontMetrics(actual);
            g.setFont(actual);
            g.setColor(selected ? Color.WHITE : DesignTokens.TEXT_SECONDARY);
            int textX = bounds.x + (bounds.width - actualMetrics.stringWidth(title)) / 2;
            int textY = bounds.y + (bounds.height - actualMetrics.getHeight()) / 2
                    + actualMetrics.getAscent();
            g.drawString(title, textX, textY);
        }

        @Override protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects,
                int index, Rectangle icon, Rectangle text, boolean selected) { }

        @Override protected void paintContentBorder(Graphics g, int placement, int selected) { }
    }
}
