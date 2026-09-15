package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** 图书馆详情弹窗的统一外壳：系统配色、圆角内容区，并相对主窗口居中。 */
final class LibraryPopup {
    private LibraryPopup() { }

    static void show(Component owner, String windowTitle, String heading,
                     String subtitle, JComponent body, Dimension size) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(owner), windowTitle,
                ModalityType.APPLICATION_MODAL);
        JButton close = new SecondaryButton("关闭");
        close.addActionListener(event -> dialog.dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(close);
        dialog.setContentPane(shell(heading, subtitle, body, footer));
        dialog.getRootPane().setDefaultButton(close);
        prepare(dialog, owner, size);
        dialog.setVisible(true);
    }

    static JPanel shell(String heading, String subtitle, JComponent body, JComponent footer) {
        JPanel root = new JPanel(new BorderLayout(0, DesignTokens.SPACE_16));
        root.setBackground(DesignTokens.PAGE_BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel icon = new JLabel(LineIcon.of(LineIcon.Kind.LIBRARY, DesignTokens.PRIMARY, 30));
        JPanel copy = new JPanel(new BorderLayout(0, 4));
        copy.setOpaque(false);
        JLabel title = UiFactory.title(heading);
        title.setForeground(DesignTokens.PRIMARY);
        copy.add(title, BorderLayout.NORTH);
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            copy.add(UiFactory.muted(subtitle), BorderLayout.SOUTH);
        }
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(icon, BorderLayout.WEST);
        header.add(copy, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        RoundedBody card = new RoundedBody();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.add(body, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        if (footer != null) root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    static void prepare(JDialog dialog, Component owner, Dimension size) {
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(Math.min(520, size.width), Math.min(420, size.height)));
        dialog.setSize(size);
        dialog.getRootPane().putClientProperty("JRootPane.titleBarBackground", DesignTokens.PAGE_BACKGROUND);
        dialog.getRootPane().putClientProperty("JRootPane.titleBarForeground", DesignTokens.TEXT_PRIMARY);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-library-popup");
        dialog.getRootPane().getActionMap().put("close-library-popup", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { dialog.dispose(); }
        });
        Window ownerWindow = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        dialog.setLocationRelativeTo(ownerWindow == null ? owner : ownerWindow);
    }

    private static final class RoundedBody extends JPanel {
        RoundedBody() { setOpaque(false); }

        @Override protected void paintComponent(Graphics original) {
            Graphics2D graphics = (Graphics2D) original.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            graphics.setColor(DesignTokens.BORDER);
            graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            graphics.dispose();
            super.paintComponent(original);
        }
    }
}
