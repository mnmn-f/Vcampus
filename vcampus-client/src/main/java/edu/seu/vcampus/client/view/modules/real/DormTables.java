package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * 表格的可读性处理：看得见的边界、分层的表头、宽表横向滚动。
 *
 * <p>扁平化做过头了——去掉外框和表头底色之后，一屏几张表挨在一起就分不出哪行属于
 * 哪张表。这里把边界加回来，但加在该加的地方：表头一层浅底色把列名和数据分开，
 * 行间一道比背景深一档的横线，整张表外面一个 1px 框收住边缘。</p>
 *
 * <p>{@link #fitColumns} 解决另一件事：列一多，Swing 默认会把每列压到几十像素，
 * 内容全变成省略号。改成按内容定宽——装得下就铺满，装不下就横向滚动。宁可滚，
 * 不要挤。</p>
 */
final class DormTables {
    /** 单列最窄，再窄列头就看不全了。 */
    private static final int MIN_COLUMN = 64;
    /** 压缩模式下的地板宽度：比 MIN_COLUMN 还窄，但至少还能看出这一列有几个字。 */
    private static final int FLOOR_COLUMN = 46;
    /** 单列最宽，避免一段长描述把整张表撑到几千像素。 */
    private static final int MAX_COLUMN = 380;
    private static final int CELL_PADDING = 26;

    private DormTables() {
    }

    /** 给表格套上宿舍模块的边界与配色。 */
    static void style(JTable table) {
        table.setBackground(Color.WHITE);
        table.setForeground(DesignTokens.TEXT_PRIMARY);
        table.setGridColor(DesignTokens.TABLE_GRID);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowHeight(36);
        // 列宽由内容决定，装不下就横向滚动；默认的等分模式会把列压成省略号。
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(false);
            header.setFont(DesignTokens.medium(13));
            header.setForeground(DesignTokens.TEXT_PRIMARY);
            header.setBackground(DesignTokens.TABLE_HEADER_BACKGROUND);
            header.setPreferredSize(new Dimension(0, 36));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.DIVIDER));
            header.setDefaultRenderer(new HeaderRenderer(header.getDefaultRenderer()));
        }
    }

    /** 表格外面的容器：1px 边框 + 白底，把这张表和相邻内容分开。 */
    static void frame(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(DesignTokens.BORDER));
        scroll.setOpaque(true);
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * 按内容给每列定宽，总宽不足时把余量补给最后一列。
     *
     * <p>先量内容再决定，是因为同一张表在不同数据下差别很大：「原因」一列可能全是
     * 空的，也可能是一段话。固定像素两种情况都不对。</p>
     *
     * @param available 表格可用宽度；小于等于 0 表示还没布局，此时只定宽不铺满
     */
    static void fitColumns(JTable table, int available) {
        fitColumns(table, available, false);
    }

    /**
     * 同上，但装不下时把列按比例压到能装下为止，而不是让表格横向滚动。
     *
     * <p>放在窄栏里的表格用这个：三等分的一栏只有六百来像素，按内容定宽必然超出，
     * 而超出的部分在 {@code BasePage} 那个禁用了横向滚动条的滚动面板里是直接被裁掉的
     * ——不是「滚一下能看到」，是彻底看不见。宁可挤，不能没。</p>
     */
    static void fitColumnsWithin(JTable table, int available) {
        fitColumns(table, available, true);
    }

    private static void fitColumns(JTable table, int available, boolean shrink) {
        TableColumnModel columns = table.getColumnModel();
        int count = columns.getColumnCount();
        if (count == 0) return;
        int total = 0;
        for (int i = 0; i < count; i++) {
            TableColumn column = columns.getColumn(i);
            int width = Math.max(MIN_COLUMN, Math.min(MAX_COLUMN, measure(table, i) + CELL_PADDING));
            column.setPreferredWidth(width);
            column.setWidth(width);
            total += width;
        }
        if (shrink && available > 0 && available < total) {
            // 按各列当前宽度等比例缩，宽的多让、窄的少让；缩到地板宽度就不再让了，
            // 剩下的差额由横向滚动条兜底（这种情况只会出现在列特别多的表上）。
            int used = 0;
            for (int i = 0; i < count; i++) {
                TableColumn column = columns.getColumn(i);
                int width = i == count - 1
                        ? Math.max(FLOOR_COLUMN, available - used)
                        : Math.max(FLOOR_COLUMN, (int) ((long) column.getPreferredWidth() * available / total));
                column.setPreferredWidth(width);
                column.setWidth(width);
                used += width;
            }
            return;
        }
        if (available > total) {
            // 余量按列数摊平，不再一股脑给最后一列——那样会把「最近归宿」这种一列撑成
            // 半屏宽的空白，整张表看起来左右拉得很长，而信息其实只有那么点。
            int extra = available - total;
            int share = extra / count;
            for (int i = 0; i < count; i++) {
                TableColumn column = columns.getColumn(i);
                int add = i == count - 1 ? extra - share * (count - 1) : share;
                column.setPreferredWidth(column.getPreferredWidth() + add);
            }
        }
    }

    private static int measure(JTable table, int columnIndex) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        int width = headerWidth(table, column, columnIndex);
        // 只量前 40 行：再往下量的收益抵不上翻页时的卡顿，而超长内容已被 MAX_COLUMN 截住。
        int rows = Math.min(table.getRowCount(), 40);
        for (int row = 0; row < rows; row++) {
            TableCellRenderer renderer = table.getCellRenderer(row, columnIndex);
            Component cell = table.prepareRenderer(renderer, row, columnIndex);
            width = Math.max(width, cell.getPreferredSize().width);
        }
        return width;
    }

    private static int headerWidth(JTable table, TableColumn column, int columnIndex) {
        JTableHeader header = table.getTableHeader();
        if (header == null) return 0;
        TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null) renderer = header.getDefaultRenderer();
        Component label = renderer.getTableCellRendererComponent(table, column.getHeaderValue(),
                false, false, -1, columnIndex);
        return label.getPreferredSize().width;
    }

    /** 表头渲染器：补上左右内边距和右侧的浅分隔线，让列名和数据列对齐。 */
    private static final class HeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        HeaderRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            Component cell = delegate.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (cell instanceof javax.swing.JComponent) {
                javax.swing.JComponent component = (javax.swing.JComponent) cell;
                component.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, DesignTokens.BORDER),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                component.setOpaque(true);
                component.setBackground(DesignTokens.TABLE_HEADER_BACKGROUND);
                component.setFont(DesignTokens.medium(13));
                component.setForeground(DesignTokens.TEXT_PRIMARY);
            }
            return cell;
        }
    }

    /** 单元格渲染器：统一内边距，选中行用主色浅底，不做斑马纹。 */
    static DefaultTableCellRenderer cellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!selected) cell.setBackground(Color.WHITE);
                return cell;
            }
        };
    }
}
