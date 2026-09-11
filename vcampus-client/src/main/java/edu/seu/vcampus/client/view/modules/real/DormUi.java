package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * 宿舍模块的排版工具：把「不用卡片」这套写法固定下来。
 *
 * <p>宿舍页面的视觉规则和其他模块不同：内容直接铺在页面底色上，靠分隔线和留白
 * 分区，只有表单和需要拎出来的提示才围一个半透明方框。规则本身很简单，但散在
 * 二十多个面板里逐个手写必然走样，所以集中放在这里。</p>
 *
 * <p>{@link #flatten(Component)} 是其中最关键的一个：现有面板全都是
 * {@code SectionCard} 和 {@code AsyncPagedTable} 拼出来的，与其把它们逐个重写，
 * 不如在页面搭好之后统一走一遍组件树，把卡片、表格、滚动面板改成扁平形态。
 * 一次调用覆盖全部页面，也不会影响其他模块——形态是每个实例自己的开关。</p>
 */
public final class DormUi {
    /**
     * 宿舍模块里每张表至少占几行高。
     *
     * <p>一张只有一两行的表夹在标题和分页条中间，看着像还没加载完；几张表并排时高度
     * 还会随各自的数据条数忽高忽低。统一垫到五行，超过五行才出滚动条。</p>
     */
    public static final int TABLE_MIN_ROWS = 5;

    /** 半透明方框的底色：白 55%，压在页面底色上比纯白轻。 */
    public static final Color PANEL_FILL = new Color(0xFF, 0xFF, 0xFF, 140);
    public static final Color PANEL_BORDER = new Color(0xDF, 0xE6, 0xE0);
    public static final Color ACCENT_FILL = new Color(0xF0, 0xF3, 0xE9, 190);

    private DormUi() {
    }

    // ---------- 扁平化 ----------

    /**
     * 把一棵组件树改成宿舍模块的扁平形态。
     *
     * <p>逐个处理：{@code SectionCard} 去掉白底与投影、改成标题上方一条分隔线；
     * 表格与它的滚动面板换成页面底色，列头退成灰字；其余白底面板一律改透明。
     * 每个容器里第一个分区不画上方分隔线，否则页面一上来就顶着一条线。</p>
     */
    public static void flatten(Component root) {
        flatten(root, new boolean[]{false});
    }

    private static void flatten(Component component, boolean[] seenSection) {
        if (component instanceof AsyncPagedTable) {
            // 每张分页表都垫到统一行高、列宽统一按可用宽度压缩。放在 flatten 里而不是
            // 逐个面板调，是因为宿舍模块的表分散在二十多个面板里，漏掉一个就会在页面上
            // 矮出一截，或者在窄栏里把右边的列顶到窗口外面。
            AsyncPagedTable<?> table = (AsyncPagedTable<?>) component;
            table.setMinVisibleRows(TABLE_MIN_ROWS);
            table.setColumnsFill(true);
        }
        if (component instanceof SectionCard) {
            SectionCard card = (SectionCard) component;
            card.flatten(seenSection[0]);
            seenSection[0] = true;
        } else if (component instanceof JTable || component instanceof JScrollPane) {
            // 表格的边框、表头和列宽全部交给 DormTables，这里不再插手：之前在这儿
            // 把表格底色改成页面底色、表头改成灰字，正是几张表糊在一起的原因。
            return;
        } else if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            // 只清白底：半透明方框和刻意上色的提示框都不是白的，保持原样。
            if (Color.WHITE.equals(panel.getBackground())) panel.setOpaque(false);
        }
        if (component instanceof JComponent) {
            // 纵向 BoxLayout 按 alignmentX 摆放子组件，而 JPanel 默认是居中——只要有
            // 一个子组件没显式设过，整页就会左右错开，看起来像「整体向右歪」。
            // alignmentX 只在纵向 BoxLayout 里起作用，其他布局忽略它，所以统一设成
            // 左对齐是安全的。
            ((JComponent) component).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        if (component instanceof Container) {
            // 每个容器内部各自从头计数，所以嵌套的分区不会互相干扰。
            boolean[] nested = component instanceof SectionCard ? seenSection : new boolean[]{false};
            Component[] children = ((Container) component).getComponents();
            for (int i = 0; i < children.length; i++) flatten(children[i], nested);
        }
    }

    /**
     * 把一棵子树里所有组件的横向对齐改成左对齐。
     *
     * <p>{@link #flatten(Component)} 只在页面搭好时走一遍，而右侧详情栏是选中一行之后
     * 现搭的——那批组件没经过 flatten，用的还是 {@code JPanel} 默认的居中对齐值。
     * 纵向 BoxLayout 会按各自的 alignmentX 把它们排成参差不齐的一列，看起来像整栏
     * 往右歪了。动态重建详情栏之后调一次这个方法即可。</p>
     */
    public static void alignLeft(Component component) {
        if (component instanceof JComponent) {
            ((JComponent) component).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        if (component instanceof Container) {
            Component[] children = ((Container) component).getComponents();
            for (int i = 0; i < children.length; i++) alignLeft(children[i]);
        }
    }

    // ---------- 文字 ----------

    public static JLabel pageTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignTokens.medium(24));
        label.setForeground(DesignTokens.TEXT_PRIMARY);
        return label;
    }

    public static JLabel sub(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignTokens.regular(12));
        label.setForeground(DesignTokens.TEXT_SECONDARY);
        return label;
    }

    /**
     * 会自动换行的一段说明文字。
     *
     * <p>{@link #sub} 是单行 JLabel，塞一段几十字的申请理由会被裁成一行末尾带省略号；
     * 表格里同样装不下。申请理由、审批意见这种整段的话就用这个：只读文本域，跟着
     * 栏宽折行，看起来仍是一段灰字而不是输入框。</p>
     */
    public static javax.swing.JTextArea paragraph(String text) {
        javax.swing.JTextArea area = new javax.swing.JTextArea(text == null ? "" : text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(DesignTokens.regular(13));
        area.setForeground(DesignTokens.TEXT_PRIMARY);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    public static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DesignTokens.regular(12));
        label.setForeground(DesignTokens.TEXT_SECONDARY);
        return label;
    }

    // ---------- 分隔 ----------

    /** 横向分隔线：区块之间用它，不用边框。 */
    public static JComponent rule() {
        return line(DesignTokens.DIVIDER, new Dimension(1, 1), true);
    }

    public static JComponent softRule() {
        return line(DesignTokens.BORDER_LIGHT, new Dimension(1, 1), true);
    }

    /** 竖分隔条：并排的统计数字、左右两栏之间用它。 */
    public static JComponent verticalRule() {
        return line(DesignTokens.DIVIDER, new Dimension(1, 1), false);
    }

    private static JComponent line(final Color color, Dimension unit, final boolean horizontal) {
        JPanel bar = new JPanel();
        bar.setOpaque(true);
        bar.setBackground(color);
        if (horizontal) {
            bar.setPreferredSize(new Dimension(1, 1));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            bar.setMinimumSize(new Dimension(0, 1));
        } else {
            bar.setPreferredSize(new Dimension(1, 1));
            bar.setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
            bar.setMinimumSize(new Dimension(1, 0));
        }
        return bar;
    }

    // ---------- 容器 ----------

    /** 纵向堆叠，元素之间留统一间距。 */
    public static JPanel stack(int gap, JComponent... items) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) continue;
            items[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            if (i > 0) panel.add(Box.createVerticalStrut(gap));
            panel.add(items[i]);
        }
        return panel;
    }

    /**
     * 等幅两栏。
     *
     * <p>刻意用 {@link GridLayout} 而不是 {@code BorderLayout} 或权重不等的
     * {@code GridBagLayout}：两栏放的是两件平级的事（比如离校申请和外来人员登记），
     * 谁宽谁窄都会读成「这个更重要」。GridLayout 强制等分，改内容也不会走样。</p>
     */
    public static JPanel columns(int gap, JComponent left, JComponent right) {
        JPanel row = new JPanel(new GridLayout(1, 2, gap, 0));
        row.setOpaque(false);
        row.add(wrap(left));
        row.add(wrap(right));
        return row;
    }

    /**
     * 等宽三栏，栏与栏之间一条竖分隔线。
     *
     * <p>{@link #split} 那套「主内容 + 固定宽侧栏」适合两件事，第三件事就摆不下了：
     * 受理住宿申请要同时看三样东西——有哪些申请、放到哪间房、这间房的床位怎么排——
     * 挤成两栏的话，房间目录和平面图就得在同一条窄栏里上下叠，图被压到屏幕外面去。
     * 三件事平级，所以三等分，谁也不比谁重要。</p>
     *
     * <p>每栏内容挂在 {@code NORTH}：三栏内容长短不一，挂 {@code CENTER} 会把短的那栏
     * 拉长，里面的表单跟着变形。</p>
     */
    public static JPanel thirds(JComponent... columns) {
        JPanel row = new JPanel(new GridLayout(1, columns.length, 0, 0));
        row.setOpaque(false);
        for (int i = 0; i < columns.length; i++) {
            // 首选宽度报 1 而不是内容的真实宽度：{@code GridLayout} 的首选宽度是
            // 「列数 × 最宽那一栏」，而每栏里都有一张按内容定宽的表，算出来轻松超过
            // 两千像素。BasePage 的滚动面板禁了横向滚动条，超出的部分不是滚不到，
            // 是直接被裁掉——最右边那一栏就这么整栏消失了。这里把宽度的话语权交还给
            // 窗口：外层 BorderLayout.NORTH 会把整行拉到窗口宽度，再三等分，栏里的
            // 表自己按拿到的宽度缩列。高度仍然照实报，否则内容会被压扁。
            JPanel cell = new JPanel(new BorderLayout()) {
                private static final long serialVersionUID = 1L;
                @Override public Dimension getPreferredSize() {
                    return new Dimension(1, super.getPreferredSize().height);
                }
                @Override public Dimension getMinimumSize() {
                    return new Dimension(1, 0);
                }
            };
            cell.setOpaque(false);
            cell.setBorder(i == 0
                    ? BorderFactory.createEmptyBorder(0, 0, 0, 20)
                    : BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 1, 0, 0, DesignTokens.DIVIDER),
                            BorderFactory.createEmptyBorder(0, 20, 0, i == columns.length - 1 ? 0 : 20)));
            if (columns[i] != null) cell.add(columns[i], BorderLayout.NORTH);
            row.add(cell);
        }
        return row;
    }

    /**
     * 左表格 + 竖分隔 + 右侧固定宽详情栏——设计稿里每一页管理界面的骨架。
     *
     * <p>右栏固定宽度而不是按比例：详情栏放的是表单和按钮，宽度需求是固定的，
     * 按比例会在宽屏上撑出一片空白、在窄屏上把输入框挤没。左栏 {@code min-width:0}
     * 让它可以被压缩，压不下的表格自己横向滚动。</p>
     */
    public static JPanel split(JComponent main, JComponent side, int sideWidth) {
        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setMinimumSize(new Dimension(0, 0));
        if (main != null) left.add(main, BorderLayout.CENTER);
        row.add(left, BorderLayout.CENTER);
        row.add(sideColumn(side, sideWidth, true), BorderLayout.EAST);
        return row;
    }

    /**
     * 侧栏容器：宽度写死，高度跟着内容走。
     *
     * <p>之前是 {@code setPreferredSize(new Dimension(width, 10))}，宽度对了，高度也
     * 一起被钉死成 10——整行的高度于是完全由表格那一侧决定，表格一短，侧栏的表单就被
     * 从下面截断，提交按钮直接看不见。宽度和高度得分开表达，所以这里只覆盖宽度。</p>
     */
    private static JPanel sideColumn(JComponent side, final int sideWidth, boolean dividerOnLeft) {
        final JPanel holder = new JPanel(new BorderLayout()) {
            @Override public Dimension getPreferredSize() {
                return new Dimension(sideWidth, super.getPreferredSize().height);
            }
            @Override public Dimension getMinimumSize() {
                return new Dimension(sideWidth, 0);
            }
        };
        holder.setOpaque(false);
        if (side != null) holder.add(side, BorderLayout.NORTH);

        JPanel dividerHolder = new JPanel(new BorderLayout());
        dividerHolder.setOpaque(false);
        dividerHolder.setBorder(BorderFactory.createEmptyBorder(0, 19, 0, 19));
        dividerHolder.add(verticalRule(), BorderLayout.CENTER);

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(holder, BorderLayout.CENTER);
        column.add(dividerHolder, dividerOnLeft ? BorderLayout.WEST : BorderLayout.EAST);
        return column;
    }

    /** 详情栏在左、主内容在右——学生端「我的住宿」按这个摆：先填申请，再看记录。 */
    public static JPanel splitLeading(JComponent side, JComponent main, int sideWidth) {
        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setOpaque(false);
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setMinimumSize(new Dimension(0, 0));
        if (main != null) right.add(main, BorderLayout.CENTER);
        row.add(right, BorderLayout.CENTER);
        row.add(sideColumn(side, sideWidth, false), BorderLayout.WEST);
        return row;
    }

    /**
     * 分区标题行：左边标题加说明，右边操作按钮。
     *
     * <p>{@code topRule} 为 true 时在上方画一条深色分隔线——两个分区之间必须看得出
     * 断点，否则一页内容会连成一片。</p>
     */
    public static JPanel header(String title, String subtitle, JComponent actions, boolean topRule) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        if (topRule) {
            JComponent line = rule();
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            block.add(line);
            block.add(Box.createVerticalStrut(18));
        }
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel head = new JLabel(title);
        head.setFont(DesignTokens.medium(17));
        head.setForeground(DesignTokens.TEXT_PRIMARY);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(head);
        if (subtitle != null && subtitle.length() > 0) {
            text.add(Box.createVerticalStrut(5));
            JLabel note = sub(subtitle);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(note);
        }
        row.add(text, BorderLayout.WEST);
        if (actions != null) row.add(actions, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        block.add(row);
        block.add(Box.createVerticalStrut(14));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        return block;
    }

    /** 一行按钮，右对齐用在 {@link #header} 的操作位。 */
    public static JPanel actions(JComponent... items) {
        JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 9, 0));
        row.setOpaque(false);
        for (JComponent item : items) if (item != null) row.add(item);
        return row;
    }

    private static JComponent wrap(JComponent content) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.setMinimumSize(new Dimension(0, 0));
        if (content != null) holder.add(content, BorderLayout.NORTH);
        return holder;
    }

    /** 半透明方框：表单和需要围起来的内容才用。 */
    public static JPanel panel() {
        return roundedBox(PANEL_FILL, PANEL_BORDER, 12, 14);
    }

    /** 强调方框（浅绿底），用于需要拎出来但不是警告的内容。 */
    public static JPanel accentPanel() {
        return roundedBox(ACCENT_FILL, DesignTokens.PRIMARY_BORDER, 12, 14);
    }

    private static JPanel roundedBox(final Color fill, final Color border, int padV, int padH) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.setColor(border);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(padV, padH, padV, padH));
        return panel;
    }

    // ---------- 统计条 ----------

    /**
     * 并排的统计数字，中间用竖线隔开。
     *
     * @param entries 每三个一组：标题、数值、单位/说明（单位可为 {@code null}）
     */
    public static JPanel stats(String... entries) {
        return stats(null, entries);
    }

    /** 同上，另给每组指定数值颜色（长度为组数，元素可为 {@code null} 表示默认色）。 */
    public static JPanel stats(Color[] valueColors, String... entries) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        int groups = entries.length / 3;
        for (int i = 0; i < groups; i++) {
            if (i > 0) {
                row.add(Box.createHorizontalStrut(28));
                row.add(verticalRule());
                row.add(Box.createHorizontalStrut(28));
            }
            Color color = valueColors != null && i < valueColors.length ? valueColors[i] : null;
            row.add(stat(entries[i * 3], entries[i * 3 + 1], entries[i * 3 + 2], color));
        }
        row.add(Box.createHorizontalGlue());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JPanel stat(String caption, String value, String unit, Color valueColor) {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        JLabel captionLabel = caption(caption);
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(captionLabel);
        cell.add(Box.createVerticalStrut(5));

        JPanel valueRow = new JPanel();
        valueRow.setOpaque(false);
        valueRow.setLayout(new BoxLayout(valueRow, BoxLayout.X_AXIS));
        JLabel number = new JLabel(value);
        number.setFont(DesignTokens.medium(24));
        number.setForeground(valueColor == null ? DesignTokens.TEXT_PRIMARY : valueColor);
        valueRow.add(number);
        if (unit != null && unit.length() > 0) {
            // 数字和单位之间留够气口：6px 时「￥60」和「1 笔待缴」几乎黏在一起。
            valueRow.add(Box.createHorizontalStrut(12));
            JLabel unitLabel = new JLabel(unit);
            unitLabel.setFont(DesignTokens.regular(13));
            unitLabel.setForeground(DesignTokens.TEXT_SECONDARY);
            unitLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
            valueRow.add(unitLabel);
        }
        valueRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(valueRow);
        return cell;
    }

    // ---------- 徽章与提示 ----------

    public enum Tone { OK, WARN, ERROR, INFO, MUTE }

    public static JLabel badge(String text, Tone tone) {
        final Color fore = foreground(tone);
        final Color back = background(tone);
        JLabel label = new JLabel(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(back);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        label.setOpaque(false);
        label.setFont(DesignTokens.medium(12));
        label.setForeground(fore);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(4, 11, 4, 11));
        return label;
    }

    /**
     * 需要拎出来的提示：左侧一条 3px 语义色，右侧标题加正文。
     *
     * <p>比整块上色克制——整块彩色底在页面上比正文还抢眼，而提示的作用是被看到，
     * 不是压住其他内容。</p>
     */
    public static JPanel notice(Tone tone, String title, String detail, JComponent action) {
        final Color fore = foreground(tone);
        JPanel box = roundedBox(background(tone), borderOf(tone), 13, 16);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel head = new JLabel(title);
        head.setFont(DesignTokens.medium(15));
        head.setForeground(fore);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(head);
        if (detail != null && detail.length() > 0) {
            text.add(Box.createVerticalStrut(6));
            JLabel body = new JLabel("<html><body style='width:520px'>" + escape(detail) + "</body></html>");
            body.setFont(DesignTokens.regular(13));
            body.setForeground(DesignTokens.TEXT_SECONDARY);
            body.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(body);
        }
        JPanel inner = new JPanel(new BorderLayout(14, 0));
        inner.setOpaque(false);
        inner.add(accentBar(fore), BorderLayout.WEST);
        inner.add(text, BorderLayout.CENTER);
        if (action != null) inner.add(action, BorderLayout.EAST);
        box.add(inner, BorderLayout.CENTER);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, box.getPreferredSize().height));
        return box;
    }

    private static JComponent accentBar(Color color) {
        JPanel bar = new JPanel();
        bar.setBackground(color);
        bar.setPreferredSize(new Dimension(3, 1));
        return bar;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Color foreground(Tone tone) {
        if (tone == Tone.OK) return DesignTokens.SUCCESS;
        if (tone == Tone.WARN) return DesignTokens.WARNING;
        if (tone == Tone.ERROR) return DesignTokens.ERROR;
        if (tone == Tone.INFO) return DesignTokens.INFO;
        return DesignTokens.TEXT_SECONDARY;
    }

    private static Color background(Tone tone) {
        if (tone == Tone.OK) return DesignTokens.SUCCESS_BACKGROUND;
        if (tone == Tone.WARN) return DesignTokens.WARNING_BACKGROUND;
        if (tone == Tone.ERROR) return DesignTokens.ERROR_BACKGROUND;
        if (tone == Tone.INFO) return DesignTokens.INFO_BACKGROUND;
        return new Color(0xFF, 0xFF, 0xFF, 150);
    }

    private static Color borderOf(Tone tone) {
        Color base = foreground(tone);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), 70);
    }

    /** 一行「标签 + 值」，用于不值得做成表格的少量字段。 */
    public static JPanel field(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        JLabel name = caption(label);
        name.setPreferredSize(new Dimension(96, 24));
        row.add(name, BorderLayout.WEST);
        row.add(UiFactory.body(value), BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
