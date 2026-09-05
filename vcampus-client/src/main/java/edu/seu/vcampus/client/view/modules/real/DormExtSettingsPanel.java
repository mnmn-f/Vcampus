package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.view.BasePage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Dimension;

/**
 * 宿舍扩展设置：运行状态、门禁时段、未归阈值和房间维护。
 *
 * <p>门禁和未归两块并排：它们都是「填两三个数字再保存」，各自撑满一整行只会在右边
 * 留下大片空白。并排之后左边缘对齐成一条线，中间一道竖分隔说明这是两组独立的设置。</p>
 *
 * <p>之前这两块看起来居中，是因为它们各自的面板用了 JPanel 默认的居中 FlowLayout；
 * 已经改成 BorderLayout，现在和页面其余内容一样从左边缘起。</p>
 */
public final class DormExtSettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    // 两块都只填两三个短数字，宽度按内容给：撑到三四百像素之后，输入框和它的标签
    // 之间会拉开一大片空白，看着像表单没填完。
    private static final int ACCESS_WIDTH = 290;
    private static final int WARNING_WIDTH = 330;

    private final DormExtSettingsStatusPanel status;
    private final DormExtSettingsAccessPanel access;
    private final DormExtSettingsWarningPanel warning;
    private final DormExtSettingsRoomPanel room;

    public DormExtSettingsPanel(BasePage page, DormExtClientService service) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        status = new DormExtSettingsStatusPanel(page, service);
        access = new DormExtSettingsAccessPanel(page, service);
        warning = new DormExtSettingsWarningPanel(page, service);
        room = new DormExtSettingsRoomPanel(page, service);

        add(status);
        add(Box.createVerticalStrut(26));
        add(DormUi.rule());
        add(Box.createVerticalStrut(26));
        add(sideBySide());
        add(Box.createVerticalStrut(26));
        add(DormUi.rule());
        add(Box.createVerticalStrut(26));
        add(room);
    }

    private JPanel sideBySide() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(column(access, ACCESS_WIDTH));
        row.add(Box.createHorizontalStrut(26));
        row.add(DormUi.verticalRule());
        row.add(Box.createHorizontalStrut(26));
        row.add(column(warning, WARNING_WIDTH));
        // 右边留白由 glue 吃掉，两块设置才会稳稳贴在左边而不是被拉宽或居中。
        row.add(Box.createHorizontalGlue());
        return row;
    }

    /**
     * 宽度写死、高度跟着内容走的一列。
     *
     * <p>不能像之前那样在构造时量一次高度再 {@code setPreferredSize} 钉死：那个数字是在
     * 面板还没进组件树、还没按 290px 布过局的时候量的，之后内容一旦比它高，多出来的
     * 部分就被切掉了——「请假期间」那一行和两个输入框的下半截正是这么没的。这里改成
     * 每次问的时候现算：宽度用常量，高度回退给 {@code BorderLayout}，它取的就是里面
     * 那块设置当前真正需要的高度。</p>
     *
     * <p>内容挂在 NORTH：最大高度等于首选高度，BoxLayout 就不会把矮的那一块拉成和
     * 高的那一块同高，里面的 {@code GridLayout} 也就不会把多出来的高度平摊给每个
     * 输入框。</p>
     */
    private static JPanel column(JPanel content, final int width) {
        JPanel holder = new JPanel(new java.awt.BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override public Dimension getPreferredSize() {
                return new Dimension(width, super.getPreferredSize().height);
            }
            @Override public Dimension getMaximumSize() {
                return new Dimension(width, super.getPreferredSize().height);
            }
            @Override public Dimension getMinimumSize() {
                return new Dimension(width, super.getPreferredSize().height);
            }
        };
        holder.setOpaque(false);
        holder.add(content, java.awt.BorderLayout.NORTH);
        return holder;
    }

    public void reload() {
        status.reload(); access.reload(); warning.reload();
    }
}
