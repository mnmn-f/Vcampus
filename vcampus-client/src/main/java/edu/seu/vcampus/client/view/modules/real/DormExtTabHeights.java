package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.components.TaskTabs;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.Dimension;

/**
 * 让标签页容器按**当前这一页**的高度伸缩，而不是按最高的那一页。
 *
 * <p>{@code JTabbedPane} 的首选尺寸取所有页里最大的一个。宿舍页面里各标签页内容量差得多
 * ——「申请与审批」叠了三块，「宿舍公告」只有两块——结果矮的页面底下会拖着一大片空白，
 * 而且外层滚动条还能一直往下滑，看着像内容没加载出来。</p>
 *
 * <p>做法是把没显示的页的首选尺寸压成 0，只让当前页报出真实高度；切页时再换过来。
 * 压的是「首选尺寸」这个提示值，不碰任何组件的实际布局，所以页面本身的排版不受影响。</p>
 */
final class DormExtTabHeights {
    private DormExtTabHeights() { }

    static void fitToSelectedTab(final TaskTabs tabs) {
        if (tabs == null) return;
        tabs.addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent event) { apply(tabs); }
        });
        apply(tabs);
    }

    private static void apply(JTabbedPane tabs) {
        int selected = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component tab = tabs.getComponentAt(i);
            if (!(tab instanceof javax.swing.JComponent)) continue;
            javax.swing.JComponent content = (javax.swing.JComponent) tab;
            // null 表示恢复成按内容自己算，Dimension(0,0) 表示「别把容器撑高」。
            content.setPreferredSize(i == selected ? null : new Dimension(0, 0));
        }
        tabs.revalidate();
        tabs.repaint();
    }
}
