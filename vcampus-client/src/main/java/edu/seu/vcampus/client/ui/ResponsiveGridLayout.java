package edu.seu.vcampus.client.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/** 按实际可用宽度决定列数；每个控件保持自身高度，供滚动表单使用。 */
public final class ResponsiveGridLayout implements LayoutManager {
    private final int minWidth;
    private final int maxColumns;
    private final int gap;

    public ResponsiveGridLayout(int minWidth, int maxColumns, int gap) {
        this.minWidth = minWidth; this.maxColumns = maxColumns; this.gap = gap;
    }
    @Override public void addLayoutComponent(String name, Component component) { }
    @Override public void removeLayoutComponent(Component component) { }
    private int columns(Container parent) {
        Insets insets = parent.getInsets();
        int width = parent.getWidth() - insets.left - insets.right;
        return Math.max(1, Math.min(maxColumns, width > 0 ? (width + gap) / (minWidth + gap) : maxColumns));
    }
    @Override public Dimension preferredLayoutSize(Container parent) {
        Insets insets = parent.getInsets(); int count = 0; int totalHeight = 0; int rowHeight = 0;
        int columns = columns(parent);
        for (Component child : parent.getComponents()) if (child.isVisible()) {
            rowHeight = Math.max(rowHeight, child.getPreferredSize().height); count++;
            if (count % columns == 0) {
                if (totalHeight > 0) totalHeight += gap;
                totalHeight += rowHeight; rowHeight = 0;
            }
        }
        if (count % columns != 0) {
            if (totalHeight > 0) totalHeight += gap;
            totalHeight += rowHeight;
        }
        return new Dimension(minWidth * columns + gap * (columns - 1) + insets.left + insets.right,
                totalHeight + insets.top + insets.bottom);
    }
    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(0, 0); }
    @Override public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets(); int columns = columns(parent); int count = 0;
        int width = Math.max(0, (parent.getWidth() - insets.left - insets.right - gap * (columns - 1)) / columns);
        for (Component child : parent.getComponents()) if (child.isVisible()) count++;
        int[] rowHeights = new int[(count + columns - 1) / columns];
        int index = 0;
        for (Component child : parent.getComponents()) if (child.isVisible()) {
            int row = index / columns;
            rowHeights[row] = Math.max(rowHeights[row], child.getPreferredSize().height); index++;
        }
        int[] rowTops = new int[rowHeights.length];
        for (int row = 1; row < rowTops.length; row++) {
            rowTops[row] = rowTops[row - 1] + rowHeights[row - 1] + gap;
        }
        index = 0;
        for (Component child : parent.getComponents()) if (child.isVisible()) {
            int row = index / columns;
            child.setBounds(insets.left + (index % columns) * (width + gap),
                    insets.top + rowTops[row], width, child.getPreferredSize().height); index++;
        }
    }
}
