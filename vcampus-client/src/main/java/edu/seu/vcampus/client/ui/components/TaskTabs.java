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
    /**
     * 标签栏是整个界面里唯一用衬线字体的地方。
     *
     * <p>它承担的是「现在在哪个板块」这件事，和下面所有正文都不是一类信息。用华文
     * 中宋把这一层单独拎出来，比继续在雅黑里加粗、调色更省力，也更容易一眼扫到。
     * 字体取不到时 {@link DesignTokens#serif(int)} 会自己往宋体、逻辑衬线体退。</p>
     */
    public TaskTabs() {
        super(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        setFont(DesignTokens.serif(17));
        setUI(new TaskTabUi());
        setForeground(DesignTokens.TEXT_PRIMARY);
        // 标签栏直接坐在页面底色上：白底加外框会在内容区外面再套一层「卡片」，
        // 而内容本身已经是铺开的，那层框只会把页面切碎。
        setBackground(DesignTokens.PAGE_BACKGROUND);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setMinimumSize(new Dimension(0, 0));
    }

    public TaskTabs addTask(String title, JComponent content) {
        JPanel canvas = new JPanel(new BorderLayout());
        canvas.setOpaque(false);
        // 左右不再留边：内容与上面的标签、外面的页边距对齐成一条线。
        canvas.setBorder(BorderFactory.createEmptyBorder(20, 0, 4, 0));
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

    /**
     * 同一标签页里的多个面板之间插一条分隔线。
     *
     * <p>只留白不画线不够：两块内容各自都有标题和表格，光靠 24px 空隙分不出「这是
     * 两件事」还是「同一件事的两段」。</p>
     */
    private void add(JPanel stack, JComponent item) {
        if (item == null) return;
        if (stack.getComponentCount() > 0) {
            stack.add(Box.createVerticalStrut(26));
            JPanel divider = new JPanel();
            divider.setBackground(DesignTokens.DIVIDER);
            divider.setPreferredSize(new Dimension(1, 1));
            divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            divider.setMinimumSize(new Dimension(0, 1));
            divider.setAlignmentX(LEFT_ALIGNMENT);
            stack.add(divider);
            stack.add(Box.createVerticalStrut(26));
        }
        item.setAlignmentX(LEFT_ALIGNMENT);
        stack.add(item);
    }

    private static final class TaskTabUi extends BasicTabbedPaneUI {
        @Override protected Insets getTabInsets(int placement, int index) {
            return new Insets(11, 20, 10, 20);
        }

        /**
         * 选中态改成「白底 + 底部绿色指示条」。
         *
         * <p>原来是整块实心绿加白字，那个体量在页面顶部太重，看起来像一排按钮而不是
         * 标签，而且标签一多就是一条刺眼的绿带。改成底部一道 3px 指示条之后，选中的
         * 是哪个仍然一目了然，但视觉重量让给了下面的内容。</p>
         */
        @Override protected void paintTabBackground(Graphics g, int placement, int index,
                int x, int y, int width, int height, boolean selected) {
            g.setColor(DesignTokens.PAGE_BACKGROUND);
            g.fillRect(x, y, width, height);
            if (selected) {
                g.setColor(DesignTokens.PRIMARY);
                g.fillRect(x, y + height - 3, width, 3);
            } else {
                // 未选中的标签底部留一道基线，让整排标签有个统一的下沿。
                g.setColor(DesignTokens.BORDER);
                g.fillRect(x, y + height - 1, width, 1);
            }
        }

        @Override protected void paintText(Graphics g, int placement, Font font,
                FontMetrics metrics, int index, String title, Rectangle bounds,
                boolean selected) {
            // 选中项加粗并用主色，替代原来「白字压在绿块上」的对比方式。
            Font actual = selected ? DesignTokens.serifBold(font.getSize()) : font;
            FontMetrics actualMetrics = g.getFontMetrics(actual);
            g.setFont(actual);
            g.setColor(selected ? DesignTokens.PRIMARY : DesignTokens.TEXT_SECONDARY);
            int x = bounds.x + (bounds.width - actualMetrics.stringWidth(title)) / 2;
            int y = bounds.y + (bounds.height - actualMetrics.getHeight()) / 2
                    + actualMetrics.getAscent();
            g.drawString(title, x, y);
        }

        @Override protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects,
                int index, Rectangle icon, Rectangle text, boolean selected) { }

        @Override protected void paintContentBorder(Graphics g, int placement, int selected) { }
    }
}
