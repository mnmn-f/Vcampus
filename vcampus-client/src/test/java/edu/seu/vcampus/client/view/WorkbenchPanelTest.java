package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 首页必须表达校园人员职责，而不是开发和部署状态。 */
public final class WorkbenchPanelTest {
    @Test public void studentAndSystemAdminReceiveDifferentHomes() {
        String student = text(new WorkbenchPanel(session(Role.STUDENT), null));
        String admin = text(new WorkbenchPanel(session(Role.SYSTEM_ADMIN), null));
        assertTrue(student.contains("工作台"));
        assertTrue(student.contains("学生"));
        assertTrue(student.contains("选课与课表"));
        assertFalse(student.contains("账号与角色"));
        assertTrue(admin.contains("工作台"));
        assertTrue(admin.contains("系统管理员"));
        assertTrue(admin.contains("账号与角色"));
        assertFalse(admin.contains("宿舍服务"));
    }

    @Test public void everyPersonnelHomeHasDistinctTitleAndNoDeveloperCopy() {
        Set<String> titles = new HashSet<String>();
        for (Role role : Role.values()) {
            String view = text(new WorkbenchPanel(session(role), null));
            titles.add(RoleWorkspace.homeTitle(role));
            assertTrue(view.contains(role.getDisplayName()));
            assertTrue(view.contains("工作台"));
            assertFalse(view.contains("演示数据"));
            assertFalse(view.contains("network"));
            assertFalse(view.contains("服务端"));
            assertFalse(view.contains("RolePolicy"));
        }
        assertTrue(titles.size() == Role.values().length);
    }

    @Test public void homeActionsHaveOneEntryPerVisibleModule() {
        for (Role role : Role.values()) {
            Set<ModuleId> modules = EnumSet.noneOf(ModuleId.class);
            for (RoleWorkspace.Action action : RoleWorkspace.actions(role)) {
                assertTrue(action.module().isVisibleTo(role));
                assertTrue("重复工作台入口: " + role + "/" + action.module(),
                        modules.add(action.module()));
            }
        }
    }

    @Test public void aiIsOfferedOnlyToStudentAndKnowledgeAdmin() {
        for (Role role : new Role[] {Role.STUDENT, Role.TEACHER, Role.AI_KNOWLEDGE_ADMIN}) {
            boolean expected = role == Role.STUDENT || role == Role.AI_KNOWLEDGE_ADMIN;
            boolean actionVisible = false;
            for (RoleWorkspace.Action action : RoleWorkspace.actions(role)) {
                if (action.module() == ModuleId.AI_ASSISTANT) {
                    actionVisible = true;
                }
            }
            assertTrue(role + " 工作台 AI 入口不符合权限策略", actionVisible == expected);
            SidebarPanel sidebar = sidebar(session(role));
            sidebar.refresh(ModuleId.DASHBOARD);
            assertTrue(role + " 侧边栏 AI 入口不符合权限策略",
                    text(sidebar).contains("校园助手") == expected);
        }
    }

    @Test public void renderStudentAndManagerHomePreviews() throws Exception {
        render("home-student-1280x820", new WorkbenchPanel(session(Role.STUDENT), null));
        render("home-academic-1280x820", new WorkbenchPanel(session(Role.ACADEMIC_ADMIN), null));
        render("home-librarian-1280x820", new WorkbenchPanel(session(Role.LIBRARIAN), null));
        render("home-dorm-manager-1280x820", new WorkbenchPanel(session(Role.DORM_MANAGER), null));
        render("home-system-admin-1280x820", new WorkbenchPanel(session(Role.SYSTEM_ADMIN), null));
    }

    private static ClientSession session(Role role) {
        ClientSession value = new ClientSession();
        value.open(new LoginResult(7L, "qa", "测试用户", role, "token-7"));
        return value;
    }

    private static String text(Component root) {
        StringBuilder value = new StringBuilder(); collect(root, value); return value.toString();
    }

    private static void collect(Component root, StringBuilder value) {
        if (root instanceof JLabel) value.append(((JLabel) root).getText()).append('\n');
        if (root instanceof AbstractButton) value.append(((AbstractButton) root).getText()).append('\n');
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) collect(child, value);
    }

    private static void render(final String name, final WorkbenchPanel panel) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                panel.setSize(1280, 820); panel.validate(); layout(panel);
                BufferedImage image = new BufferedImage(1280, 820, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
                try { File dir = new File("target/ui-previews"); dir.mkdirs(); ImageIO.write(image, "png", new File(dir, name + ".png")); }
                catch (Exception ex) { throw new PreviewFailure(ex); }
            }
        });
    }

    private static SidebarPanel sidebar(final ClientSession session) {
        return new SidebarPanel(new SidebarPanel.ClientSessionView() {
            @Override public Role activeRole() { return session.getActiveRole(); }
        }, new SidebarPanel.Listener() { @Override public void onModuleSelected(ModuleId moduleId) { } });
    }

    private static void layout(Component root) {
        root.doLayout();
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) layout(child);
    }

    private static final class PreviewFailure extends RuntimeException {
        PreviewFailure(Exception cause) { super(cause); }
    }
}
