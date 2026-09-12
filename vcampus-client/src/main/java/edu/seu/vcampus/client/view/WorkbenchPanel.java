package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

/** 根据当前校园身份呈现不同任务入口的首页。 */
public final class WorkbenchPanel extends BasePage {
    public interface NavigationHandler { void open(ModuleId moduleId); }
    private final NavigationHandler navigation;

    public WorkbenchPanel(ClientSession session, NavigationHandler navigation) {
        this(session, navigation, null, null);
    }

    public WorkbenchPanel(ClientSession session, NavigationHandler navigation,
                          ClientBusinessServices services) {
        this(session, navigation, services, null);
    }

    public WorkbenchPanel(ClientSession session, NavigationHandler navigation,
                          ClientBusinessServices services, Runnable passwordChanged) {
        super(session, "工作台",
                "");
        this.navigation = navigation;
        setHeaderContext(dateText());
        addBlock(hero(session));
        addBlock(actionGrid(session.getActiveRole()));
    }

    private JPanel hero(ClientSession current) {
        Role role = current.getActiveRole();
        JPanel hero = new JPanel(new BorderLayout(24, 0));
        hero.setBackground(DesignTokens.PRIMARY);
        hero.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));
        hero.setPreferredSize(new Dimension(0, 112));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel greeting = new JLabel(current.getDisplayName());
        greeting.setFont(DesignTokens.medium(25));
        greeting.setForeground(Color.WHITE);
        String account = current.getLoginResult() == null
                ? "" : current.getLoginResult().getAccount();
        JLabel line = new JLabel("校园账号：" + (account == null ? "" : account));
        line.setFont(DesignTokens.regular(14));
        line.setForeground(new Color(0xDF, 0xF2, 0xEA));
        words.add(greeting);
        words.add(Box.createVerticalStrut(9));
        words.add(line);
        hero.add(words, BorderLayout.CENTER);
        JLabel identity = new JLabel(role.getDisplayName());
        identity.setOpaque(true);
        identity.setBackground(new Color(0x78, 0x8E, 0x4D));
        identity.setForeground(Color.WHITE);
        identity.setFont(DesignTokens.medium(13));
        identity.setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));
        JPanel badge = new JPanel(new java.awt.GridBagLayout());
        badge.setOpaque(false);
        badge.add(identity);
        hero.add(badge, BorderLayout.EAST);
        return hero;
    }

    private SectionCard actionGrid(Role role) {
        SectionCard section = new SectionCard("常用事项", "");
        int count = RoleWorkspace.actions(role).size();
        JPanel grid = new JPanel(new GridLayout(0, Math.min(3, count), 12, 12));
        grid.setOpaque(false);
        for (RoleWorkspace.Action action : RoleWorkspace.actions(role)) grid.add(actionCard(action));
        section.setContent(grid);
        return section;
    }

    private JButton actionCard(final RoleWorkspace.Action action) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout(8, 6));
        button.setHorizontalAlignment(JButton.LEFT);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesignTokens.BORDER),
                BorderFactory.createEmptyBorder(16, 17, 16, 17)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel title = UiFactory.sectionTitle(action.title());
        JLabel description = UiFactory.muted(action.description());
        JLabel arrow = new JLabel("›");
        arrow.setFont(DesignTokens.medium(24));
        arrow.setForeground(DesignTokens.PRIMARY);
        button.add(title, BorderLayout.NORTH);
        button.add(description, BorderLayout.CENTER);
        JLabel icon = new JLabel(LineIcon.of(action.module(), DesignTokens.PRIMARY));
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
        button.add(icon, BorderLayout.WEST);
        button.add(arrow, BorderLayout.EAST);
        button.setPreferredSize(new Dimension(220, 86));
        button.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { open(action.module()); }
        });
        return button;
    }

    private void open(ModuleId module) { if (navigation != null) navigation.open(module); }

    private static String dateText() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEEE",
                java.util.Locale.CHINA));
    }
}
