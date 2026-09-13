package edu.seu.vcampus.client.ui.components;


import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Insets;

/** 表格内容区：按记录数收缩，避免少量数据占满大片空白。 */
public final class TableViewport extends JPanel {
    private static final String TABLE = "table";
    private static final String EMPTY = "empty";
    private final CardLayout cards = new CardLayout();
    private final JScrollPane tableScroll;
    private final JTable table;
    private final EmptyStatePanel empty = new EmptyStatePanel("暂无记录", "", null);
    /**
     * 表格至少占几行高。
     *
     * <p>默认 1 是「有几条就多高」；宿舍模块统一设成 5，因为一张只有两行的表夹在
     * 标题和分页条中间，看起来像是没加载完。设成固定值之后，一页里几张表的高度也
     * 不会随数据条数忽高忽低。</p>
     */
    private int minVisibleRows = 1;
    /** 空态自己的首选高度，只量一次——setPreferredSize 之后就再也量不到原值了。 */
    private int emptyNaturalHeight = -1;
    /** 当前要露出几行；滚动面板的高度按它算，而不是写死。 */
    private int visibleRows = 1;

    public TableViewport(JTable table) {
        super();
        setLayout(cards);
        setOpaque(false);
        this.table = table;
        tableScroll = new JScrollPane(table) {
            /**
             * 高度 = 边框 + 表头 + 要露出的行数 × 行高（+ 横向滚动条，如果它会出现）。
             *
             * <p>之前写死在 {@code showRows} 里、没算边框那 2px，于是表格身体比视口高 2px，
             * 竖滚动条冒出来；竖滚动条又占掉 16px 宽，铺满宽度的列就装不下了，横滚动条
             * 也跟着冒出来盖住最后一行——一张五行的表长出两根滚动条。改成每次布局时
             * 现算，滚动条会不会出现也按当时的宽度判断，不再提前猜。</p>
             */
            @Override public Dimension getPreferredSize() {
                Insets insets = getInsets();
                int rowHeight = Math.max(1, table.getRowHeight());
                JTableHeader header = table.getTableHeader();
                int headerHeight = header == null ? rowHeight
                        : Math.max(rowHeight, header.getPreferredSize().height);
                int height = insets.top + insets.bottom + headerHeight + visibleRows * rowHeight;
                if (needsHorizontalBar()) {
                    JScrollBar bar = getHorizontalScrollBar();
                    height += bar == null ? 16 : bar.getPreferredSize().height;
                }
                return new Dimension(800, height);
            }

            private boolean needsHorizontalBar() {
                if (getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) return false;
                int width = getWidth();
                if (width <= 0) return false;
                Insets insets = getInsets();
                int room = width - insets.left - insets.right;
                JScrollBar vertical = getVerticalScrollBar();
                if (vertical != null && vertical.isVisible()) room -= vertical.getWidth();
                return table.getPreferredSize().width > room;
            }
        };
        tableScroll.setColumnHeaderView(table.getTableHeader());
        // 表格外面套一个 1px 框：一屏里几张表挨着时，没有边界就分不出哪行属于哪张表。
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(0xD9, 0xE1, 0xDD)));
        tableScroll.getViewport().setBackground(Color.WHITE);
        // 列宽由内容决定，装不下就横向滚动——挤成省略号比多一条滚动条难用得多。
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(tableScroll, TABLE);
        add(empty, EMPTY);
        showLoading();
    }

    /** 设定表格最少占几行高；改完后下一次 {@link #showRows(int)} 生效。 */
    public void setMinVisibleRows(int rows) {
        minVisibleRows = Math.max(1, rows);
    }

    public int getMinVisibleRows() {
        return minVisibleRows;
    }

    public void showRows(int count) {
        visibleRows = Math.max(minVisibleRows, Math.min(Math.max(10, minVisibleRows), count));
        // 行高跟着 Table.rowHeight 走，写死 38 会在改了行高之后多留出一条空白。
        int rowHeight = Math.max(1, table.getRowHeight());
        table.setPreferredScrollableViewportSize(new Dimension(800, Math.max(1, count) * rowHeight));
        // 这一页的行数装得下就不要竖滚动条：固定几行一页的表本来就该一屏见底，
        // 留着 AS_NEEDED 会因为 1、2px 的误差冒出一根滚动条，把列宽也挤乱。
        tableScroll.setVerticalScrollBarPolicy(count > visibleRows
                ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED : JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        cards.show(this, TABLE);
        revalidate();
    }

    public void showEmpty(String title, String reason) {
        empty.setCopy(title, reason);
        // 空态和表格同高，否则一搜索、一翻页整页内容会上下跳一大截。取两者的大值：
        // 默认那一行的模块不受影响，空态该多高还是多高。
        if (emptyNaturalHeight < 0) emptyNaturalHeight = empty.getPreferredSize().height;
        int rowHeight = Math.max(1, table.getRowHeight());
        empty.setPreferredSize(new Dimension(800,
                Math.max(emptyNaturalHeight, rowHeight * (minVisibleRows + 1))));
        cards.show(this, EMPTY);
        revalidate();
    }

    /** 表格当前的可用宽度，供列宽自适应使用；还没布局时返回 0。 */
    public int tableWidth() {
        return tableScroll.getViewport().getWidth();
    }

    public void showLoading() {
        showEmpty("正在更新", "");
    }

    public void showError() {
        showEmpty("暂时无法显示", "请检查连接后点击“重试”。");
    }
}
