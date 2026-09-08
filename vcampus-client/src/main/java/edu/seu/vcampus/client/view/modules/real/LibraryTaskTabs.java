package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;
import edu.seu.vcampus.client.ui.WidthTrackingPanel;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** 固定导航与页头；每个任务独立纵向滚动。 */
public final class LibraryTaskTabs extends JPanel {
    private final JPanel rail = new JPanel(new BorderLayout());
    private final JPanel brand = LibraryUi.stack(5);
    private final JPanel navigation = LibraryUi.stack(9);
    private final JPanel pages = new JPanel(new CardLayout());
    private final JLabel breadcrumb = LibraryUi.muted("图书馆 / 首页");
    private final JLabel user = LibraryUi.label("", 13, false);
    private final JLabel footer = LibraryUi.muted("VCampus");
    private final List<JButton> buttons = new ArrayList<>();
    private final List<LineIcon.Kind> icons = new ArrayList<>();
    private int selected; private boolean compact;

    public LibraryTaskTabs(boolean librarian) {
        super(new BorderLayout()); setBackground(DesignTokens.PAGE_BACKGROUND); setMinimumSize(new Dimension(0, 0));
        rail.setBackground(Color.WHITE); rail.setPreferredSize(new Dimension(190, 0));
        rail.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DesignTokens.BORDER_LIGHT));
        brand.setBorder(BorderFactory.createEmptyBorder(22, 18, 26, 12));
        JLabel name = LibraryUi.label("<html>东南大学<br>图书馆</html>", 17, true);
        name.setIcon(LineIcon.of(LineIcon.Kind.LIBRARY, LibraryUi.GREEN, 34)); name.setIconTextGap(10); brand.add(name);
        if (librarian) brand.add(LibraryUi.muted("管理员端"));
        rail.add(brand, BorderLayout.NORTH);
        navigation.setBorder(BorderFactory.createEmptyBorder(10, 10, 16, 10)); rail.add(navigation, BorderLayout.CENTER);
        footer.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 16)); rail.add(footer, BorderLayout.SOUTH);
        add(rail, BorderLayout.WEST);
        JPanel workspace = new JPanel(new BorderLayout()); workspace.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout()); header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER_LIGHT), BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        user.setIcon(LineIcon.of(LineIcon.Kind.PROFILE, LibraryUi.GREEN, 25)); user.setIconTextGap(9);
        header.add(breadcrumb, BorderLayout.CENTER); header.add(user, BorderLayout.EAST); workspace.add(header, BorderLayout.NORTH);
        pages.setOpaque(false); workspace.add(pages, BorderLayout.CENTER); add(workspace, BorderLayout.CENTER);
    }
    public void setDisplayName(String name) { user.setText(name); }
    public LibraryTaskTabs addTask(String title, LineIcon.Kind icon, JComponent first, JComponent... rest) {
        final int index = buttons.size();
        JButton b = LibraryUi.button(title, false, () -> selectTask(index));
        b.setHorizontalAlignment(SwingConstants.LEFT); b.setIconTextGap(10);
        b.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        buttons.add(b); icons.add(icon); navigation.add(b);
        JPanel stack = LibraryUi.stack(18); stack.add(first); if (rest != null) for (JComponent c : rest) stack.add(c);
        stack.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel canvas = new JPanel(new BorderLayout()); canvas.setOpaque(false); canvas.add(stack, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(new WidthTrackingPanel(canvas));
        scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setBackground(DesignTokens.PAGE_BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24); pages.add(scroll, String.valueOf(index));
        if (index == 0) selectTask(0); return this;
    }
    public void selectTask(int index) {
        if (index < 0 || index >= buttons.size()) return; selected = index;
        for (int i = 0; i < buttons.size(); i++) {
            JButton b = buttons.get(i); boolean active = i == index;
            b.putClientProperty("library.active", active);
            b.setForeground(active ? LibraryUi.GREEN : LibraryUi.INK);
            b.setIcon(LineIcon.of(icons.get(i), active ? LibraryUi.GREEN : LibraryUi.MUTED, 20));
            b.setFont(active ? DesignTokens.medium(14) : DesignTokens.regular(14)); b.repaint();
        }
        breadcrumb.setText("图书馆 / " + buttons.get(index).getText());
        ((CardLayout) pages.getLayout()).show(pages, String.valueOf(index));
    }
    public int selectedTask() { return selected; }
    @Override public void doLayout() {
        boolean narrow = getWidth() > 0 && getWidth() < 900;
        if (narrow != compact) {
            compact = narrow; remove(rail); brand.setVisible(!narrow); footer.setVisible(!narrow);
            navigation.setLayout(narrow ? new edu.seu.vcampus.client.ui.WrapLayout(6) : new LibraryUi.StackLayout(9));
            rail.setPreferredSize(narrow ? null : new Dimension(190, 0)); add(rail, narrow ? BorderLayout.NORTH : BorderLayout.WEST);
        }
        super.doLayout();
    }
}
