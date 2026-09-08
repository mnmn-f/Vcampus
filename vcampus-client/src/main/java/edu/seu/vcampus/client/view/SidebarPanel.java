package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Map;

/** 只负责导航展示与按角色过滤，不创建业务页面。 */
public final class SidebarPanel extends JPanel {
    public interface Listener {
        void onModuleSelected(ModuleId moduleId);
    }

    private final ClientSessionView session;
    private final Listener listener;
    private final JPanel navigation = new JPanel();
    private final Map<ModuleId, JButton> buttons = new EnumMap<ModuleId, JButton>(ModuleId.class);
    private final JLabel roleLabel = new JLabel("当前身份 · —");
    private ModuleId active = ModuleId.DASHBOARD;

    public SidebarPanel(ClientSessionView session, Listener listener) {
        super(new BorderLayout());
        this.session = session;
        this.listener = listener;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(238, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DesignTokens.BORDER));
        add(buildBrand(), BorderLayout.NORTH);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBackground(Color.WHITE);
        navigation.setBorder(BorderFactory.createEmptyBorder(14, 12, 18, 12));
        JScrollPane scroll = new JScrollPane(navigation);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    public void refresh(ModuleId activeModule) {
        active = activeModule == null ? ModuleId.DASHBOARD : activeModule;
        navigation.removeAll();
        buttons.clear();
        Role role = readRole();
        roleLabel.setText(role == null ? "当前身份 · —" : "当前身份 · " + role.getDisplayName());
        String previousGroup = null;
        for (ModuleId module : ModuleId.values()) {
            if (role == null || !module.isVisibleTo(role)) {
                continue;
            }
            String group = RoleWorkspace.group(module);
            if (!group.equals(previousGroup)) {
                navigation.add(groupLabel(group));
                navigation.add(Box.createVerticalStrut(4));
                previousGroup = group;
            }
            final ModuleId target = module;
            JButton button = new JButton(RoleWorkspace.navigationLabel(role, target));
            style(button);
            button.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    listener.onModuleSelected(target);
                }
            });
            buttons.put(module, button);
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(4));
        }
        if (buttons.isEmpty()) {
            navigation.add(UiFactory.muted("暂无可用功能"));
        }
        updateSelection();
        navigation.revalidate();
        navigation.repaint();
    }

    public void setActive(ModuleId activeModule) {
        active = activeModule;
        updateSelection();
    }

    private JPanel buildBrand() {
        JPanel brand = new JPanel(new BorderLayout(0, 12));
        brand.setBackground(DesignTokens.PRIMARY);
        brand.setBorder(BorderFactory.createEmptyBorder(22, 22, 20, 18));
        JPanel mark = new JPanel(new BorderLayout(9, 0));
        mark.setOpaque(false);
        mark.add(new JLabel(LineIcon.brand(Color.WHITE, 26)), BorderLayout.WEST);
        JLabel name = new JLabel("VCampus");
        name.setFont(DesignTokens.medium(24));
        name.setForeground(Color.WHITE);
        mark.add(name, BorderLayout.CENTER);
        brand.add(mark, BorderLayout.NORTH);
        JLabel subtitle = new JLabel("东南大学校园服务");
        subtitle.setFont(DesignTokens.regular(12));
        subtitle.setForeground(new Color(0xD3, 0xEF, 0xE2));
        JPanel details = new JPanel(new BorderLayout(0, 8));
        details.setOpaque(false);
        details.add(subtitle, BorderLayout.NORTH);
        roleLabel.setFont(DesignTokens.regular(12));
        roleLabel.setForeground(Color.WHITE);
        roleLabel.setOpaque(true);
        roleLabel.setBackground(DesignTokens.PRIMARY_HOVER);
        roleLabel.setBorder(BorderFactory.createEmptyBorder(6, 9, 6, 9));
        details.add(roleLabel, BorderLayout.SOUTH);
        brand.add(details, BorderLayout.SOUTH);
        return brand;
    }

    private JLabel groupLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setFont(DesignTokens.medium(11));
        label.setForeground(DesignTokens.TEXT_SECONDARY);
        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 5, 4));
        return label;
    }

    private void style(JButton button) {
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setPreferredSize(new Dimension(208, 42));
        button.setFont(DesignTokens.regular(14));
        button.setForeground(DesignTokens.TEXT_PRIMARY);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(0, 11, 0, 10));
        button.setIconTextGap(10);
        button.setUI(new BasicButtonUI());
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setRolloverEnabled(true);
    }

    private void updateSelection() {
        for (Map.Entry<ModuleId, JButton> entry : buttons.entrySet()) {
            boolean selected = entry.getKey() == active;
            entry.getValue().setBackground(selected ? DesignTokens.PRIMARY : Color.WHITE);
            entry.getValue().setForeground(selected ? Color.WHITE : DesignTokens.TEXT_PRIMARY);
            entry.getValue().setIcon(LineIcon.of(entry.getKey(),
                    selected ? Color.WHITE : DesignTokens.PRIMARY));
            entry.getValue().setFont(selected ? DesignTokens.medium(14) : DesignTokens.regular(14));
            entry.getValue().setBorder(selected
                    ? BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 3, 0, 0, DesignTokens.GOLD),
                            BorderFactory.createEmptyBorder(0, 8, 0, 10))
                    : BorderFactory.createEmptyBorder(0, 11, 0, 10));
        }
    }

    private Role readRole() {
        try {
            return session.activeRole();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    /** 仅抽取导航需要的会话读取能力，避免 Sidebar 依赖完整会话写操作。 */
    public interface ClientSessionView {
        Role activeRole();
    }
}
