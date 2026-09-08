package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;

/** 宿舍设置卡片的公共布局和按钮样式。 */
final class DormExtSettingsSupport {
    private DormExtSettingsSupport() { }

    /**
     * 一块设置：字段在上，按钮在下。
     *
     * <p>字段挂在 NORTH 而不是 CENTER：这两块设置和旁边的内容并排，容器给的高度往往
     * 比它们需要的多，挂在 CENTER 的 {@code GridLayout} 会把多出来的高度平摊到每一格，
     * 于是「落锁时间」那个只填五个字符的输入框被拉成了一个方块。NORTH 只按首选高度
     * 摆，多余的高度留给空着的 CENTER。</p>
     */
    static JPanel wrap(SectionCard card, JPanel fields, JButton primary, JButton secondary) {
        JPanel actions = UiFactory.horizontal(8);
        actions.add(secondary);
        actions.add(primary);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(fields, BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(top, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel result = new JPanel(new BorderLayout());
        result.setOpaque(false);
        result.add(card, BorderLayout.NORTH);
        return result;
    }

    /**
     * 让一段说明在窄栏里折行。
     *
     * <p>{@code JLabel} 不会自己折行，宽度不够就从中间截断加省略号——一句「改动即时
     * 生效，历史记录按新时段重新判定」被截成「改动即时生效...」之后，读的人根本不知道
     * 改了会发生什么。包成 HTML 并给定宽度，Swing 的 HTML 渲染就会折行。</p>
     */
    static String wrapText(String text, int width) {
        return "<html><body style='width:" + width + "px'>"
                + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                + "</body></html>";
    }

    static JButton button(String label, boolean primary, Runnable action) {
        JButton result = primary ? new PrimaryButton(label) : new SecondaryButton(label);
        result.addActionListener((ActionListener) event -> action.run());
        return result;
    }
}
