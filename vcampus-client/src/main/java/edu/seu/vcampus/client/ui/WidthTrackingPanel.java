package edu.seu.vcampus.client.ui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

/** 滚动容器总是跟随视口宽度，内容变高时只增加纵向滚动。 */
public final class WidthTrackingPanel extends JPanel implements Scrollable {
    public WidthTrackingPanel(JComponent content) {
        super(new BorderLayout()); setOpaque(false); add(content, BorderLayout.CENTER);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) {
                revalidate(); content.revalidate();
            }
        });
    }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(16, visible.height - 32); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
