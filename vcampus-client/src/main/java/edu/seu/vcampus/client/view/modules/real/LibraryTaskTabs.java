package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 图书馆专用的左侧任务导航。
 *
 * <p>设计说明中的学生端和管理员端都使用左侧导航，因此不再沿用通用模块的
 * 顶部页签。业务面板仍然复用已有实现，只调整页面组合和视觉层级。</p>
 */
public final class LibraryTaskTabs extends JPanel {
    // 图书馆只复用系统视觉令牌，不修改或覆盖全局颜色主题。
    private static final Color ACCENT = DesignTokens.PRIMARY;
    private static final Color ACCENT_LIGHT = DesignTokens.PRIMARY_LIGHT;
    private static final Color TEXT = DesignTokens.TEXT_PRIMARY;
    private static final Color BORDER = DesignTokens.BORDER_LIGHT;
    private static final Color CANVAS = DesignTokens.PAGE_BACKGROUND;

    private final JPanel navigation = new JPanel();
    private final JPanel pages = new JPanel(new CardLayout());
    private final List<NavigationButton> buttons = new ArrayList<NavigationButton>();

    public LibraryTaskTabs(boolean librarian) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(BORDER));
        setMinimumSize(new Dimension(0, 0));

        JPanel rail = new JPanel(new BorderLayout());
        rail.setBackground(Color.WHITE);
        rail.setPreferredSize(new Dimension(178, 0));
        rail.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        JPanel brand = new JPanel();
        brand.setBackground(Color.WHITE);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(24, 20, 18, 16));
        JLabel title = new JLabel("东南大学图书馆");
        title.setFont(DesignTokens.medium(16));
        title.setForeground(TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        brand.add(title);
        if (librarian) {
            brand.add(Box.createVerticalStrut(5));
            JLabel role = new JLabel("管理员端");
            role.setFont(DesignTokens.medium(13));
            role.setForeground(DesignTokens.TEXT_SECONDARY);
            role.setAlignmentX(LEFT_ALIGNMENT);
            brand.add(role);
        }
        rail.add(brand, BorderLayout.NORTH);

        navigation.setBackground(Color.WHITE);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(BorderFactory.createEmptyBorder(8, 16, 24, 16));
        rail.add(navigation, BorderLayout.CENTER);

        pages.setBackground(CANVAS);
        pages.setMinimumSize(new Dimension(0, 0));
        add(rail, BorderLayout.WEST);
        add(pages, BorderLayout.CENTER);
    }

    public LibraryTaskTabs addTask(String title, LineIcon.Kind icon, JComponent first,
                                   JComponent... rest) {
        final int index = buttons.size();
        final String cardName = "library-task-" + index;
        NavigationButton button = new NavigationButton(title, icon);
        button.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { select(index, cardName); }
        });
        buttons.add(button);
        navigation.add(button);
        navigation.add(Box.createVerticalStrut(12));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        addToStack(stack, first);
        if (rest != null) for (JComponent item : rest) addToStack(stack, item);

        JPanel canvas = new JPanel(new BorderLayout());
        canvas.setBackground(CANVAS);
        canvas.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        canvas.setMinimumSize(new Dimension(0, 0));
        canvas.add(stack, BorderLayout.NORTH);
        pages.add(canvas, cardName);

        if (index == 0) select(0, cardName);
        revalidate();
        repaint();
        return this;
    }

    private void addToStack(JPanel stack, JComponent item) {
        if (item == null) return;
        item.setAlignmentX(LEFT_ALIGNMENT);
        stack.add(item);
        stack.add(Box.createVerticalStrut(16));
    }

    private void select(int index, String cardName) {
        for (int i = 0; i < buttons.size(); i++) buttons.get(i).setSelectedTask(i == index);
        ((CardLayout) pages.getLayout()).show(pages, cardName);
    }

    private static final class NavigationButton extends JButton {
        private final LineIcon.Kind kind;
        private boolean selectedTask;

        private NavigationButton(String text, LineIcon.Kind kind) {
            super(text);
            this.kind = kind == null ? LineIcon.Kind.DASHBOARD : kind;
            setUI(new BasicButtonUI());
            setFont(DesignTokens.medium(14));
            setForeground(TEXT);
            setBackground(Color.WHITE);
            setIcon(LineIcon.of(this.kind, TEXT, 20));
            setIconTextGap(12);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(13, 14, 13, 14)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setPreferredSize(new Dimension(146, 50));
            setFocusPainted(false);
            setContentAreaFilled(true);
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) {
                    if (!selectedTask) setBackground(ACCENT_LIGHT);
                }
                @Override public void mouseExited(MouseEvent event) { applyState(); }
            });
        }

        private void setSelectedTask(boolean selected) {
            selectedTask = selected;
            applyState();
        }

        private void applyState() {
            setForeground(selectedTask ? ACCENT : TEXT);
            setBackground(selectedTask ? ACCENT_LIGHT : Color.WHITE);
            setIcon(LineIcon.of(kind, selectedTask ? ACCENT : TEXT, 20));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selectedTask ? ACCENT : BORDER),
                    BorderFactory.createEmptyBorder(13, 14, 13, 14)));
        }

    }
}
