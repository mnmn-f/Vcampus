package edu.seu.vcampus.client.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/** 按实际可用宽度决定列数；高度随行数变化，供滚动页面使用。 */
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
        Insets insets = parent.getInsets(); int height = 0; int count = 0;
        for (Component child : parent.getComponents()) if (child.isVisible()) {
            height = Math.max(height, child.getPreferredSize().height); count++;
        }
        int columns = columns(parent); int rows = (count + columns - 1) / columns;
        return new Dimension(minWidth * columns + gap * (columns - 1) + insets.left + insets.right,
                rows * height + Math.max(0, rows - 1) * gap + insets.top + insets.bottom);
    }
    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(0, 0); }
    @Override public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets(); int columns = columns(parent); int index = 0;
        int width = Math.max(0, (parent.getWidth() - insets.left - insets.right - gap * (columns - 1)) / columns);
        int height = 0;
        for (Component child : parent.getComponents()) if (child.isVisible()) height = Math.max(height, child.getPreferredSize().height);
        for (Component child : parent.getComponents()) if (child.isVisible()) {
            child.setBounds(insets.left + (index % columns) * (width + gap),
                    insets.top + (index / columns) * (height + gap), width, height); index++;
        }
    }
}
