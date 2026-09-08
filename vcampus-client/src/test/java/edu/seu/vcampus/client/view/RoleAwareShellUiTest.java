package edu.seu.vcampus.client.view;
import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.auth.ClientServiceException;
import edu.seu.vcampus.client.controller.LoginController;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.LineIcon;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.util.EnumSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
/** 外壳和登录入口的角色可见性与面向用户文案门禁。 */
public final class RoleAwareShellUiTest {
    @Test
    public void sidebarOnlyShowsCurrentRoleBusinessAndActiveState() {
        ClientSession student = session(EnumSet.of(Role.STUDENT), Role.STUDENT, "林同学");
        SidebarPanel studentPanel = sidebar(student);
        studentPanel.refresh(ModuleId.ACADEMIC);
        String studentText = text(studentPanel);
        assertTrue(studentText.contains("选课与课表"));
        assertTrue(studentText.contains("校园业务"));
        assertFalse(studentText.contains("账号与角色"));
        JButtonCheck active = button(studentPanel, "选课与课表");
        assertNotNull(active.button);
        assertEquals(DesignTokens.PRIMARY, active.button.getBackground());
        assertTrue(active.button.getIcon() instanceof LineIcon);
        ClientSession admin = session(EnumSet.of(Role.SYSTEM_ADMIN), Role.SYSTEM_ADMIN, "系统管理员");
        SidebarPanel adminPanel = sidebar(admin);
        adminPanel.refresh(ModuleId.SYSTEM);
        String adminText = text(adminPanel);
        assertTrue(adminText.contains("账号与角色"));
        assertTrue(adminText.contains("安全与运行"));
        assertFalse(adminText.contains("宿舍生活"));
        assertFalse(adminText.contains("校园商店"));
    }
    @Test
    public void sidebarUsesServerDetectedRoleForDifferentNavigation() {
        assertRoleMenu(Role.STUDENT, "选课与课表", "宿舍生活", "账号与角色");
        assertRoleMenu(Role.TEACHER, "我的教学", "图书馆", "宿舍管理");
        assertRoleMenu(Role.ACADEMIC_ADMIN, "教务管理", "校园业务", "账号与角色");
    }

    private static void assertRoleMenu(Role role, String present, String alsoPresent,
                                        String absent) {
        ClientSession current = session(EnumSet.of(role), role, "已识别人员");
        SidebarPanel sidebar = sidebar(current);
        sidebar.refresh(ModuleId.DASHBOARD);
        String view = text(sidebar);
        assertTrue(view.contains(present));
        assertTrue(view.contains(alsoPresent));
        assertFalse(view.contains(absent));
    }

    @Test
    public void topBarShowsRoleNameAndKeepsRoleSwitching() {
        Set<Role> roles = EnumSet.of(Role.STUDENT, Role.DORM_MANAGER);
        final ClientSession session = session(roles, Role.STUDENT, "多职责用户");
        TopBarPanel topbar = new TopBarPanel(new StubAuth(), new TopBarPanel.SessionView() {
            @Override public Set<Role> roles() { return session.getRoles(); }
            @Override public Role activeRole() { return session.getActiveRole(); }
            @Override public String displayName() { return session.getDisplayName(); }
            @Override public String account() { return session.getAccount(); }
            @Override public long userId() { return session.getUserId(); }
        }, new TopBarPanel.Listener() {
            @Override public void onRoleChanged(Role role) { }
            @Override public void onLogout() { }
        });
        topbar.setActiveModule(ModuleId.DORMITORY);
        String view = text(topbar);
        assertTrue(view.contains("当前工作身份"));
        assertTrue(view.contains("学生"));
        assertTrue(view.contains("多职责用户"));
        assertTrue(view.contains("qa"));
        assertTrue(view.contains("7"));
        assertTrue(view.contains("姓名"));
        assertTrue(view.contains("校园账号"));
        assertTrue(view.contains("用户编号"));
        assertTrue(view.contains("宿舍生活"));
        assertFalse(view.contains("演示模式"));
        assertFalse(view.contains("已连接"));
        assertFalse(view.contains("network"));
        JComboBox<?> selector = find(topbar, JComboBox.class);
        assertNotNull(selector);
        assertEquals(2, selector.getItemCount());
    }

    @Test
    public void profileDisplayNameRefreshesTopBarAndWorkbench() {
        final ClientSession session = session(EnumSet.of(Role.STUDENT), Role.STUDENT, "旧显示名");
        AppShell shell = new AppShell(new StubAuth(), session, new AppShell.Listener() {
            @Override public void onLogout() { }
        }, null);
        session.updateDisplayName("新显示名");
        String view = text(shell);
        assertTrue(view.contains("新显示名"));
        assertFalse(view.contains("旧显示名"));
    }

    @Test
    public void loginOnlyCollectsCredentialsAndDoesNotChooseIdentity() {
        LoginFormPanel form = new LoginFormPanel(new LoginController(new StubAuth()), new LoginFormPanel.Listener() {
            @Override public void onLoginSuccess(LoginResult result) { }
        });
        String view = text(form) + text(new LoginBrandPanel());
        assertTrue(view.contains("校园账号 / 学号 / 工号"));
        assertTrue(view.contains("登录密码"));
        assertTrue(view.contains("登录"));
        assertFalse(view.contains("选择进入身份"));
        assertFalse(view.contains("体验账号"));
        assertFalse(view.contains("学生"));
        assertFalse(view.contains("宿管员"));
        assertFalse(view.contains("系统管理员"));
        assertFalse(view.contains("注册账号"));
        assertFalse(view.contains("忘记密码"));
        assertFalse(view.contains("欢迎回来"));
        assertFalse(view.contains("一站办理"));
        assertFalse(view.contains("自然 · 确定 · 有意义 · 成长"));
        assertFalse(view.contains("字段错误"));
        assertFalse(view.contains("本地联调"));
        assertFalse(view.contains("真实环境"));
        assertFalse(view.contains("network"));
        assertNotNull(button(form, "登录").button);
        assertEquals("", form.getAccountField().getText());
        assertEquals(0, form.getPasswordField().getPassword().length);
    }

    @Test
    public void closedRegistrationRouteIsOptionalAndHasNoIdentityChooser() {
        final boolean[] opened = {false};
        LoginFormPanel form = new LoginFormPanel(new LoginController(new StubAuth()), new LoginFormPanel.Listener() {
            @Override public void onLoginSuccess(LoginResult result) { }
        }, new Runnable() {
            @Override public void run() { opened[0] = true; }
        });
        JButtonCheck register = button(form, "注册账号");
        assertNotNull(register.button);
        register.button.doClick();
        assertTrue(opened[0]);
        assertFalse(text(form).contains("选择进入身份"));
    }

    private static ClientSession session(Set<Role> roles, Role active, String name) {
        ClientSession value = new ClientSession();
        value.open(new LoginResult(7L, "qa", name, roles, active, "token-7"));
        return value;
    }

    private static SidebarPanel sidebar(final ClientSession session) {
        return new SidebarPanel(new SidebarPanel.ClientSessionView() {
            @Override public Role activeRole() { return session.getActiveRole(); }
        }, new SidebarPanel.Listener() { @Override public void onModuleSelected(ModuleId moduleId) { } });
    }

    private static String text(Component root) {
        StringBuilder value = new StringBuilder(); collect(root, value); return value.toString();
    }

    private static void collect(Component root, StringBuilder value) {
        if (root instanceof JLabel) value.append(((JLabel) root).getText()).append('\n');
        if (root instanceof AbstractButton) value.append(((AbstractButton) root).getText()).append('\n');
        if (root instanceof JTextField) value.append(((JTextField) root).getText()).append('\n');
        if (root instanceof Container)
            for (Component child : ((Container) root).getComponents()) collect(child, value);
    }

    private static JButtonCheck button(Component root, String label) {
        if (root instanceof AbstractButton && label.equals(((AbstractButton) root).getText()))
            return new JButtonCheck((AbstractButton) root);
        if (root instanceof Container)
            for (Component child : ((Container) root).getComponents()) {
                JButtonCheck found = button(child, label);
                if (found.button != null) return found;
            }
        return new JButtonCheck(null);
    }

    private static <T> T find(Component root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Container)
            for (Component child : ((Container) root).getComponents()) {
                T found = find(child, type); if (found != null) return found;
            }
        return null;
    }

    private static final class JButtonCheck {
        private final AbstractButton button;
        private JButtonCheck(AbstractButton button) { this.button = button; }
    }

    private static final class StubAuth implements AuthClientService {
        @Override public LoginResult login(String account, String password) {
            return new LoginResult(7L, account, "测试用户", Role.STUDENT, "token");
        }
        @Override public void logout() { }
        @Override public Role switchRole(Role role) throws ClientServiceException { return role; }
        @Override public boolean isLoggedIn() { return true; }
    }
}
