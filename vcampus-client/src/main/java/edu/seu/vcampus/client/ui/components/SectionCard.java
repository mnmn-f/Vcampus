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

/**
 * 内容分区，统一标题、说明和主体内边距。
 *
 * <p>有两种形态。默认是白色卡片，其他模块一直这么用。宿舍模块改用
 * {@linkplain #flatten(boolean) 扁平形态}：去掉白底、描边和投影，内容直接铺在页面
 * 底色上，只靠标题上方的一条分隔线分区——满屏白卡片会把页面切成一格一格，信息
 * 反而更难扫。形态是每个实例自己的开关，所以宿舍改扁平不会动到别人的页面。</p>
 */
public class SectionCard extends JPanel {
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel heading;
    private boolean flat;
    private boolean topRule = true;

    public SectionCard(String title, String subtitle) {
        super(new BorderLayout(0, DesignTokens.SPACE_12));
        setOpaque(false);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(17, 17, 17, 17));

        heading = new JPanel(new BorderLayout(0, 4));
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

    /**
     * 切成扁平形态：不画白底、描边和投影，改成标题上方一条分隔线。
     *
     * @param withTopRule 是否画上方分隔线；每一页的第一个分区应传 {@code false}，
     *                    否则页面一上来就顶着一条线。
     */
    public void flatten(boolean withTopRule) {
        flat = true;
        topRule = withTopRule;
        setOpaque(false);
        // 扁平之后左右不再缩进，分区标题与页面其余内容对齐成一条竖线。
        setBorder(BorderFactory.createEmptyBorder(withTopRule ? 6 : 0, 0, 0, 0));
        heading.setBorder(BorderFactory.createEmptyBorder(withTopRule ? 16 : 0, 0, 0, 0));
        revalidate();
        repaint();
    }

    public boolean isFlat() {
        return flat;
    }

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /**
     * 卡片投影。
     *
     * <p>之前卡片只有一道 {@code BORDER_LIGHT} 的描边，而它和页面底色的明度差不到
     * 4%，肉眼几乎看不见边界，整页会糊成一片白。这里改成「浅投影 + 略深描边」：
     * 投影负责把卡片从背景上抬起来，描边负责收住边缘，两者都很克制，不会显脏。</p>
     *
     * <p>投影用三层逐渐变淡的圆角矩形叠出来——Swing 没有原生阴影，高斯模糊又太重，
     * 三层描边在这个尺度下肉眼已经分辨不出层次。</p>
     */
    private static final int SHADOW_LAYERS = 3;

    @Override protected void paintComponent(Graphics graphics) {
        if (flat) {
            if (topRule) {
                graphics.setColor(DesignTokens.BORDER);
                graphics.fillRect(0, 0, getWidth(), 1);
            }
            super.paintComponent(graphics);
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = DesignTokens.RADIUS * 2;
        int w = getWidth();
        int h = getHeight();
        for (int i = SHADOW_LAYERS; i >= 1; i--) {
            // 越外层越淡；向下偏移 1px，让光源看起来在上方。
            g.setColor(new Color(0x2A, 0x33, 0x25, 6 + (SHADOW_LAYERS - i) * 4));
            g.drawRoundRect(i, i + 1, w - 1 - i * 2, h - 2 - i * 2, arc, arc);
        }
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 0, w - 1 - SHADOW_LAYERS, h - 1 - SHADOW_LAYERS, arc, arc);
        g.dispose();
        super.paintComponent(graphics);
    }

    @Override protected void paintBorder(Graphics graphics) {
        if (flat) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(DesignTokens.BORDER);
        g.drawRoundRect(0, 0, getWidth() - 1 - SHADOW_LAYERS,
                getHeight() - 1 - SHADOW_LAYERS,
                DesignTokens.RADIUS * 2, DesignTokens.RADIUS * 2);
        g.dispose();
    }
}
