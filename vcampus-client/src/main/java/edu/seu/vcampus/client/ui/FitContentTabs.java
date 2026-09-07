package edu.seu.vcampus.client.ui;

import javax.swing.JTabbedPane;
import java.awt.Dimension;

/** 页签高度跟随当前页，避免隐藏的长表单撑出大片空白。 */
public final class FitContentTabs extends JTabbedPane {
    public FitContentTabs() {
        super(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        addChangeListener(event -> { revalidate(); if (getParent() != null) getParent().revalidate(); });
    }
    @Override public Dimension getPreferredSize() {
        Dimension result = super.getPreferredSize();
        if (getSelectedComponent() == null) return result;
        int maximum = 0;
        for (int i = 0; i < getTabCount(); i++) maximum = Math.max(maximum, getComponentAt(i).getPreferredSize().height);
        return new Dimension(result.width, Math.max(1, result.height - maximum + getSelectedComponent().getPreferredSize().height));
    }
}
