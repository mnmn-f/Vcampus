package edu.seu.vcampus.client.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import javax.swing.JComponent;
import javax.swing.JTable;

/** 全页面表格统一禁止移动列，包括后来加载的卡片和弹出的子面板。 */
public final class TableInteractionPolicy {
    private static final String INSTALLED = TableInteractionPolicy.class.getName();
    private TableInteractionPolicy() { }

    public static void install(Component component) {
        if (!(component instanceof JComponent)) return;
        JComponent target = (JComponent) component;
        if (Boolean.TRUE.equals(target.getClientProperty(INSTALLED))) return;
        target.putClientProperty(INSTALLED, true);
        if (target instanceof JTable) {
            JTable table = (JTable) target;
            lock(table);
            table.addPropertyChangeListener("tableHeader", e -> lock(table));
        }
        for (Component child : target.getComponents()) install(child);
        target.addContainerListener(new ContainerAdapter() {
            @Override public void componentAdded(ContainerEvent e) { install(e.getChild()); }
        });
    }
    private static void lock(JTable table) {
        if (table.getTableHeader() != null) table.getTableHeader().setReorderingAllowed(false);
    }
}
