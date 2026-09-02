package edu.seu.vcampus.client.view.modules.real;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;

/**
 * 把 main 的「日常治理」面板拆成未归和卫生两半。
 *
 * <p>{@code DormManagerGovernancePanel} 在一个纵向面板里依次放了「未归预警表 + 处理未归」
 * 和「卫生检查表 + 检查表单」四块，是两件不相干的事挤在一个标签页里。要分成两个标签页，
 * 本来得改那个类，但它属于 main。</p>
 *
 * <p>这里改成不动源码的做法：照常构造那个面板，然后把它的子组件**移**到两个新容器里
 * ——Swing 的 {@code add} 会自动把组件从原父容器摘下来，而组件内部的按钮、监听器和数据
 * 加载都只引用它自己的字段，跟父容器是谁无关，所以搬家之后功能完全不变。</p>
 *
 * <p>切分点取「第二个表格」的位置，而不是写死下标 2：这样即使 main 以后在某一半里多加
 * 一块卡片，切分依然正确。万一结构变得认不出来（找不到第二个表格），就退化成整块不拆，
 * 界面会回到合并前的样子，而不是丢东西。</p>
 */
final class DormExtGovernanceSplit {
    private final JPanel absence = column();
    private final JPanel hygiene = column();
    private final boolean split;

    DormExtGovernanceSplit(JPanel governance) {
        Component[] children = governance.getComponents();
        int boundary = secondTableIndex(children);
        this.split = boundary > 0;
        for (int i = 0; i < children.length; i++) {
            JPanel target = split && i >= boundary ? hygiene : absence;
            if (children[i] instanceof JComponent) {
                ((JComponent) children[i]).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            target.add(children[i]);
        }
    }

    /** 未归那一半：预警列表和处理动作。 */
    JPanel absencePart() { return absence; }

    /** 卫生那一半；结构不符合预期时返回 null，调用方按「没有这块」处理。 */
    JPanel hygienePart() { return split ? hygiene : null; }

    private static int secondTableIndex(Component[] children) {
        int seen = 0;
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof AsyncPagedTable && ++seen == 2) return i;
        }
        return -1;
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMinimumSize(new Dimension(0, 0));
        return panel;
    }
}
