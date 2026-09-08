package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.common.dto.library.BookDetail;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.function.IntConsumer;
/** 图书馆学生工作区的局部视觉组件，不覆盖其他模块的 Swing 样式。 */
final class LibraryUi {
    static final Color GREEN = DesignTokens.PRIMARY;
    static final Color SOFT = DesignTokens.PRIMARY_LIGHT;
    static final Color INK = DesignTokens.TEXT_PRIMARY;
    static final Color MUTED = DesignTokens.TEXT_SECONDARY;
    private LibraryUi() { }
    static JPanel stack(int gap) {
        JPanel p = new JPanel(new StackLayout(gap)); p.setOpaque(false);
        p.setMinimumSize(new Dimension(0, 0)); return p;
    }
    static JPanel row(int gap) {
        JPanel p = new JPanel(new edu.seu.vcampus.client.ui.WrapLayout(gap));
        p.setOpaque(false); return p;
    }
    static JPanel between(Component left, Component right) {
        JPanel p = new JPanel(new BorderLayout(12, 0)); p.setOpaque(false);
        p.add(left, BorderLayout.CENTER); if (right != null) p.add(right, BorderLayout.EAST); return p;
    }
    static JPanel columns(Component left, Component right) {
        JPanel p = new JPanel(new ColumnsLayout()); p.setOpaque(false); p.add(left); p.add(right); return p;
    }
    static Surface card() { return new Surface(Color.WHITE, 18); }
    static JLabel label(String text, int size, boolean bold) {
        JLabel l = new JLabel(text); l.setFont(bold ? DesignTokens.medium(size) : DesignTokens.regular(size));
        l.setForeground(INK); l.setMinimumSize(new Dimension(0, l.getPreferredSize().height)); return l;
    }
    static JLabel muted(String text) { JLabel l = label(text, 12, false); l.setForeground(MUTED); return l; }
    static JLabel heading(String title) { return label(title, 26, true); }
    static JLabel badge(String text, boolean available) {
        JLabel l = label(text, 12, false); l.setOpaque(true);
        l.setBackground(available ? SOFT : new Color(0xF0F2EF));
        l.setForeground(available ? GREEN : MUTED); l.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9)); return l;
    }
    static JButton button(String text, boolean primary, Runnable action) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics original) {
                Graphics2D g = (Graphics2D) original.create(); quality(g);
                Color bg = !isEnabled() ? new Color(0xE8ECE5) : primary
                        ? (getModel().isRollover() ? DesignTokens.PRIMARY_HOVER : GREEN)
                        : (getModel().isRollover() || Boolean.TRUE.equals(getClientProperty("library.active")) ? SOFT : Color.WHITE);
                g.setColor(bg); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
                if (!primary || hasFocus()) { g.setColor(hasFocus() ? GREEN : DesignTokens.PRIMARY_BORDER); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9); }
                g.dispose(); super.paintComponent(original);
            }
        };
        b.setUI(new BasicButtonUI()); b.setFont(DesignTokens.regular(13));
        b.setForeground(primary ? Color.WHITE : GREEN); b.setOpaque(false); b.setContentAreaFilled(false);
        b.setFocusPainted(false); b.setRolloverEnabled(true); b.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run()); return b;
    }
    static JButton link(String text, Runnable action) {
        JButton b = UiFactory.linkButton(text); b.setUI(new BasicButtonUI()); b.addActionListener(e -> action.run());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    static JTextField search(String placeholder) {
        JTextField f = new JTextField(1) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    g.setColor(DesignTokens.TEXT_PLACEHOLDER); g.setFont(getFont());
                    g.drawString(placeholder, getInsets().left, (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
                }
            }
        };
        UiFactory.styleField(f); f.setPreferredSize(new Dimension(180, 39));
        f.getAccessibleContext().setAccessibleName(placeholder); return f;
    }
    static <T> JComboBox<T> combo(T[] values) {
        JComboBox<T> c = new JComboBox<>(values); c.setFont(DesignTokens.regular(13)); c.setBackground(Color.WHITE);
        c.setForeground(INK); c.setPreferredSize(new Dimension(145, 36)); return c;
    }
    static JPanel field(String title, JComponent value) {
        JPanel p = stack(7); p.add(muted(title)); p.add(value); return p;
    }
    static JPanel state(String text, Runnable retry) {
        JPanel p = stack(10); p.setBorder(BorderFactory.createEmptyBorder(28, 12, 28, 12));
        p.add(muted(text)); if (retry != null) p.add(between(new JLabel(), link("重新加载", retry))); return p;
    }
    static void replace(JPanel container, Component... children) {
        container.removeAll(); for (Component child : children) container.add(child);
        container.revalidate(); container.repaint();
    }
    static JPanel pager(int page, int size, long total, IntConsumer navigate) {
        long count = Math.max(1, (total + size - 1) / size);
        JPanel controls = row(4);
        JButton previous = button("‹", false, () -> navigate.accept(page - 1)); previous.setEnabled(page > 1); controls.add(previous);
        int start = Math.max(1, page - 1), end = (int) Math.min(count, start + 3);
        for (int n = start; n <= end; n++) { final int number = n; controls.add(button(String.valueOf(n), n == page, () -> navigate.accept(number))); }
        JButton next = button("›", false, () -> navigate.accept(page + 1)); next.setEnabled(page < count); controls.add(next);
        return between(muted("共 " + total + " 项"), controls);
    }
    static boolean borrowable(BookDetail b) { return "ON_SHELF".equals(b.getStatus()) && b.getAvailableCopies() > 0; }
    static String plain(String value) { return value == null || value.isBlank() ? "—" : value; }
    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    static void styleLegacy(Component component) {
        if (component instanceof JTable) {
            JTable t = (JTable) component; t.setRowHeight(40); t.setShowGrid(false);
            t.setFont(DesignTokens.regular(13)); t.setSelectionBackground(SOFT);
            t.getTableHeader().setFont(DesignTokens.medium(12)); t.getTableHeader().setBackground(SOFT);
        }
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) styleLegacy(child);
    }
    static final class Surface extends JPanel {
        private final Color fill;
        Surface(Color fill, int padding) { super(new StackLayout(14)); this.fill = fill; setOpaque(false); setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding)); }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create(); quality(g);
            g.setColor(fill); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g.setColor(DesignTokens.BORDER_LIGHT); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14); g.dispose();
            super.paintComponent(original);
        }
    }
    /** Title-based fallback covers use actual catalog metadata, never fictitious book images. */
    static final class Cover extends JComponent {
        private final String title, author; private final Color color; private final BufferedImage image;
        Cover(BookDetail b, int width, int height) { this(b.getTitle(), b.getAuthor(), b.getCoverImage(), width, height); }
        Cover(String title, String author, byte[] bytes, int width, int height) {
            this.title = plain(title); this.author = author == null ? "" : author;
            Color[] colors = {new Color(0x6A7754), new Color(0x916447), new Color(0x35534A), new Color(0x85764F)};
            color = colors[Math.floorMod(this.title.hashCode(), colors.length)];
            BufferedImage decoded = null;
            if (bytes != null) try { decoded = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes)); } catch (Exception ignored) { }
            image = decoded; setPreferredSize(new Dimension(width, height)); setMinimumSize(getPreferredSize());
            getAccessibleContext(); setToolTipText(this.title);
        }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g = (Graphics2D) original.create(); quality(g); int w = getWidth(), h = getHeight();
            g.setClip(new RoundRectangle2D.Float(0, 0, w, h, 7, 7));
            if (image != null) {
                g.setColor(SOFT); g.fillRect(0, 0, w, h);
                double scale = Math.min(w / (double) image.getWidth(), h / (double) image.getHeight());
                int iw = (int) (image.getWidth() * scale), ih = (int) (image.getHeight() * scale);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            } else {
                g.setColor(color); g.fillRect(0, 0, w, h); g.setColor(new Color(255, 255, 255, 35));
                g.fillRect(5, 0, 2, h); g.drawOval(w / 3, h * 2 / 3, w, w); g.drawOval(-w / 2, h * 3 / 4, w * 2, w);
                g.setColor(new Color(0xFAF6E9)); g.setFont(DesignTokens.medium(w > 130 ? 22 : 15));
                int y = Math.max(26, h / 4); StringBuilder line = new StringBuilder();
                for (int i = 0; i < title.length(); i++) {
                    char c = title.charAt(i);
                    if (g.getFontMetrics().stringWidth(line.toString() + c) > w - 24 && line.length() > 0) {
                        g.drawString(line.toString(), 12, y); y += g.getFontMetrics().getHeight() + 2; line.setLength(0);
                    }
                    if (y > h - 35) break; line.append(c);
                }
                if (y <= h - 30) g.drawString(line.toString(), 12, y);
                g.setFont(DesignTokens.regular(10)); String a = author;
                while (!a.isEmpty() && g.getFontMetrics().stringWidth(a) > w - 24) a = a.substring(0, a.length() - 1);
                g.drawString(a, 12, h - 20);
            }
            g.dispose();
        }
    }
    static final class StackLayout implements LayoutManager {
        private final int gap; StackLayout(int gap) { this.gap = gap; }
        public void addLayoutComponent(String n, Component c) { } public void removeLayoutComponent(Component c) { }
        public Dimension minimumLayoutSize(Container p) { return new Dimension(0, 0); }
        public Dimension preferredLayoutSize(Container p) {
            Insets in = p.getInsets(); int width = 0, height = 0, count = 0;
            for (Component c : p.getComponents()) if (c.isVisible()) { Dimension d = c.getPreferredSize(); width = Math.max(width, d.width); height += d.height; count++; }
            return new Dimension(width + in.left + in.right, height + Math.max(0, count - 1) * gap + in.top + in.bottom);
        }
        public void layoutContainer(Container p) {
            Insets in = p.getInsets(); int y = in.top, width = Math.max(0, p.getWidth() - in.left - in.right);
            for (Component c : p.getComponents()) if (c.isVisible()) { c.setSize(width, c.getHeight()); int h = c.getPreferredSize().height; c.setBounds(in.left, y, width, h); y += h + gap; }
        }
    }
    static final class ColumnsLayout implements LayoutManager {
        public void addLayoutComponent(String n, Component c) { } public void removeLayoutComponent(Component c) { }
        public Dimension minimumLayoutSize(Container p) { return new Dimension(0, 0); }
        public Dimension preferredLayoutSize(Container p) {
            Component a = p.getComponent(0), b = p.getComponent(1);
            return new Dimension(920, p.getWidth() > 0 && p.getWidth() < 900 ? a.getPreferredSize().height + b.getPreferredSize().height + 18 : Math.max(a.getPreferredSize().height, b.getPreferredSize().height));
        }
        public void layoutContainer(Container p) {
            int w = p.getWidth(); Component a = p.getComponent(0), b = p.getComponent(1);
            if (w < 900) { a.setSize(w, a.getHeight()); b.setSize(w, b.getHeight()); int ah = a.getPreferredSize().height; a.setBounds(0, 0, w, ah); b.setBounds(0, ah + 18, w, b.getPreferredSize().height); }
            else { int right = Math.max(270, Math.min(340, w / 3)); a.setSize(w - right - 18, a.getHeight()); b.setSize(right, b.getHeight()); a.setBounds(0, 0, w - right - 18, a.getPreferredSize().height); b.setBounds(w - right, 0, right, b.getPreferredSize().height); }
        }
    }
}
