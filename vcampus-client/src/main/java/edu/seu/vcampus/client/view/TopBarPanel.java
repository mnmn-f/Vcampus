package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** 顶栏：当前职责切换、用户身份和退出登录。 */
public final class TopBarPanel extends JPanel {
    public interface Listener {
        void onRoleChanged(Role role);
        void onLogout();
    }

    private final SessionView session;
    private final Listener listener;
    private final JComboBox<Role> roleSelector = new JComboBox<Role>();
    private final JLabel pageTitle = UiFactory.sectionTitle("校园服务");
    private final JLabel identityValue = UiFactory.body("—");
    private final JLabel nameValue = UiFactory.body("—");
    private final JLabel accountValue = UiFactory.body("—");
    private ModuleId activeModule = ModuleId.DASHBOARD;
    private boolean changing;

    public TopBarPanel(AuthClientService authService, SessionView session, Listener listener) {
        super(new BorderLayout());
        this.session = session;
        this.listener = listener;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));
        setPreferredSize(new Dimension(0, 66));
        add(buildLeft(), BorderLayout.WEST);
        add(buildRight(), BorderLayout.EAST);
    }

    public void syncRole() {
        changing = true;
        roleSelector.removeAllItems();
        java.util.Set<Role> roles = session.roles();
        if (roles != null) {
            for (Role role : roles) {
                roleSelector.addItem(role);
            }
        }
        Role role = readRole();
        roleSelector.setSelectedItem(role);
        boolean canSwitch = roles != null && roles.size() > 1;
        roleSelector.setEnabled(canSwitch);
        roleSelector.setVisible(canSwitch);
        identityValue.setText(role == null ? "—" : role.getDisplayName());
        pageTitle.setText(role == null ? "校园服务"
                : RoleWorkspace.navigationLabel(role, activeModule));
        String name = readName();
        nameValue.setText(name == null || name.trim().length() == 0 ? "—" : name);
        String account = readAccount();
        accountValue.setText(account == null || account.trim().length() == 0 ? "—" : account);
        changing = false;
    }

    public void setActiveModule(ModuleId module) {
        activeModule = module == null ? ModuleId.DASHBOARD : module;
        Role role = readRole();
        pageTitle.setText(role == null ? "校园服务"
                : RoleWorkspace.navigationLabel(role, activeModule));
    }

    private JPanel buildLeft() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(new JLabel(LineIcon.brand(DesignTokens.PRIMARY, 22)));
        pageTitle.setFont(DesignTokens.medium(16));
        left.add(pageTitle);
        return left;
    }

    private JPanel buildRight() {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(identityBlock());
        roleSelector.setFont(DesignTokens.regular(13));
        roleSelector.setPreferredSize(new Dimension(128, 34));
        roleSelector.setToolTipText("切换工作身份");
        roleSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
                        selected, focused);
                if (value instanceof Role) {
                    label.setText(((Role) value).getDisplayName());
                }
                return label;
            }
        });
        syncRole();
        roleSelector.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { roleChanged(); }
        });
        right.add(roleSelector);
        right.add(nameBlock());
        right.add(accountBlock());
        JButton logout = new SecondaryButton("退出登录");
        logout.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { listener.onLogout(); }
        });
        right.add(logout);
        return right;
    }

    private JPanel nameBlock() {
        JPanel block = new JPanel(new BorderLayout(0, 1));
        block.setOpaque(false);
        block.add(UiFactory.muted("姓名"), BorderLayout.NORTH);
        nameValue.setFont(DesignTokens.medium(14));
        block.add(nameValue, BorderLayout.SOUTH);
        return block;
    }

    private JPanel accountBlock() {
        JPanel block = new JPanel(new BorderLayout(0, 1));
        block.setOpaque(false);
        block.add(UiFactory.muted("校园账号"), BorderLayout.NORTH);
        accountValue.setFont(DesignTokens.medium(13));
        block.add(accountValue, BorderLayout.SOUTH);
        return block;
    }

    private JPanel identityBlock() {
        JPanel block = new JPanel(new java.awt.BorderLayout(0, 1));
        block.setOpaque(false);
        JLabel caption = UiFactory.muted("当前工作身份");
        block.add(caption, java.awt.BorderLayout.NORTH);
        identityValue.setFont(DesignTokens.medium(13));
        identityValue.setForeground(DesignTokens.PRIMARY);
        block.add(identityValue, java.awt.BorderLayout.SOUTH);
        return block;
    }

    private void roleChanged() {
        if (!changing && listener != null && roleSelector.getSelectedItem() instanceof Role) {
            listener.onRoleChanged((Role) roleSelector.getSelectedItem());
        }
    }

    private Role readRole() {
        try { return session.activeRole(); }
        catch (IllegalStateException ignored) { return null; }
    }

    private String readName() {
        try { return session.displayName(); }
        catch (IllegalStateException ignored) { return null; }
    }

    private String readAccount() {
        try { return session.account(); }
        catch (IllegalStateException ignored) { return null; }
    }

    public interface SessionView {
        java.util.Set<Role> roles();
        Role activeRole();
        String displayName();
        String account();
        long userId();
    }
}
