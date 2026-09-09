package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.ui.LineIcon;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 图书馆二级任务统一使用全站顶部标签，不再维护独立左侧栏。 */
public final class LibraryTaskTabs extends JPanel {
    private final TaskTabs tabs = new TaskTabs();

    public LibraryTaskTabs(boolean librarian) {
        super(new BorderLayout()); setOpaque(false); add(tabs, BorderLayout.CENTER);
    }

    public void setDisplayName(String name) { /* 用户信息由全局 TopBarPanel 统一展示。 */ }

    public LibraryTaskTabs addTask(String title, LineIcon.Kind icon,
                                    JComponent first, JComponent... rest) {
        tabs.addTask(title, first, rest); return this;
    }

    public void selectTask(int index) {
        if (index >= 0 && index < tabs.getTabCount()) tabs.setSelectedIndex(index);
    }

    public int selectedTask() { return tabs.getSelectedIndex(); }
}
