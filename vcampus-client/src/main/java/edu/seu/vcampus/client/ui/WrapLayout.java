package edu.seu.vcampus.client.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/** FlowLayout 自动换行时同时报告真实高度，避免第二行被裁剪。 */
public final class WrapLayout extends FlowLayout {
    public WrapLayout(int gap) { super(FlowLayout.LEFT, gap, gap); }
    @Override public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets(); int width = parent.getWidth();
            if (width <= 0) width = 800;
            int max = Math.max(1, width - insets.left - insets.right - getHgap() * 2);
            int rowWidth = 0, rowHeight = 0, totalHeight = 0, totalWidth = 0;
            for (Component child : parent.getComponents()) if (child.isVisible()) {
                Dimension size = child.getPreferredSize();
                if (rowWidth > 0 && rowWidth + getHgap() + size.width > max) {
                    totalWidth = Math.max(totalWidth, rowWidth); totalHeight += rowHeight + getVgap();
                    rowWidth = 0; rowHeight = 0;
                }
                if (rowWidth > 0) rowWidth += getHgap();
                rowWidth += size.width; rowHeight = Math.max(rowHeight, size.height);
            }
            return new Dimension(Math.max(totalWidth, rowWidth) + insets.left + insets.right + getHgap() * 2,
                    totalHeight + rowHeight + insets.top + insets.bottom + getVgap() * 2);
        }
    }
    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(0, 0); }
}
