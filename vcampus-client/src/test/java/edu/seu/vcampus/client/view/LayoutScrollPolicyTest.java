package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.auth.AuthClientService;
import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.components.TaskTabs;
import edu.seu.vcampus.client.ui.components.DataTableToolbar;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 验证页面只承担外层滚动，标签页不会再包一层重复滚动。 */
public final class LayoutScrollPolicyTest {
    @Test public void academicPageKeepsOneOuterVerticalScrollAtCompactWidth() throws Exception {
        check(ModuleId.ACADEMIC, Role.STUDENT, 1100, 720);
    }

    @Test public void identityPageHasNoHorizontalOverflowAtDeliveryWidth() throws Exception {
        check(ModuleId.USER_ADMIN, Role.SYSTEM_ADMIN, 1280, 820);
    }

    @Test public void compactAcademicActionsStayInsideTheirToolbar() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
            ClientSession session = new ClientSession();
            session.open(new LoginResult(11L, "preview", "预览用户", Role.STUDENT, "preview-token"));
            AppShell shell = new AppShell(new PreviewAuth(), session, new AppShell.Listener() {
                @Override public void onLogout() { }
            }, null, services(session));
            shell.setSize(1100, 720); shell.open(ModuleId.ACADEMIC); layout(shell);
            for (DataTableToolbar toolbar : all(shell, DataTableToolbar.class)) {
                for (JButton button : buttons(toolbar)) {
                    java.awt.Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(),
                            button.getBounds(), toolbar);
                    assertTrue("toolbar action is clipped", bounds.x >= 0 && bounds.y >= 0
                            && bounds.x + bounds.width <= toolbar.getWidth()
                            && bounds.y + bounds.height <= toolbar.getHeight());
                }
            }
            }
        });
    }

    private void check(final ModuleId module, final Role role, final int width, final int height) throws Exception {
        final JScrollPane[] pageScroll = new JScrollPane[1];
        final TaskTabs[] tabs = new TaskTabs[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
            ClientSession session = new ClientSession();
            session.open(new LoginResult(9L, "preview", "预览用户", role, "preview-token"));
            AppShell shell = new AppShell(new PreviewAuth(), session, new AppShell.Listener() {
                @Override public void onLogout() { }
            }, null, services(session));
            shell.setSize(width, height); shell.open(module); layout(shell);
            BasePage page = find(shell, BasePage.class);
            assertNotNull(page); pageScroll[0] = findPageScroll(page);
            tabs[0] = find(page, TaskTabs.class);
            assertNotNull(tabs[0]);
            assertFalse(pageScroll[0].getHorizontalScrollBar().isVisible());
            }
        });
        assertNotNull(pageScroll[0]);
        assertTrue(pageScroll[0].getVerticalScrollBar().isVisible()
                || width > 1100);
        for (int i = 0; i < tabs[0].getTabCount(); i++) {
            assertFalse(tabs[0].getComponentAt(i) instanceof JScrollPane);
        }
    }

    private static JScrollPane findPageScroll(BasePage page) {
        for (Component child : page.getComponents()) {
            if (child instanceof JScrollPane) return (JScrollPane) child;
        }
        return null;
    }

    private static <T> T find(Container root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        for (Component child : root.getComponents()) {
            if (child instanceof Container) {
                T result = find((Container) child, type);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static <T> java.util.List<T> all(Container root, Class<T> type) {
        java.util.List<T> result = new java.util.ArrayList<T>();
        if (type.isInstance(root)) result.add(type.cast(root));
        for (Component child : root.getComponents()) if (child instanceof Container) result.addAll(all((Container) child, type));
        return result;
    }

    private static java.util.List<JButton> buttons(Container root) {
        java.util.List<JButton> result = new java.util.ArrayList<JButton>();
        for (Component child : root.getComponents()) {
            if (child instanceof JButton) result.add((JButton) child);
            if (child instanceof Container) result.addAll(buttons((Container) child));
        }
        return result;
    }

    private static void layout(Component root) {
        root.doLayout();
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) layout(child);
    }

    private static ClientBusinessServices services(ClientSession session) {
        return new ClientBusinessServices(new NetworkClientService(new EmptyGateway()), session);
    }

    private static final class PreviewAuth implements AuthClientService {
        @Override public LoginResult login(String account, String password) { return null; }
        @Override public void logout() { }
        @Override public Role switchRole(Role role) { return role; }
        @Override public boolean isLoggedIn() { return true; }
    }

    private static final class EmptyGateway implements ClientGateway {
        @Override public Message send(Message request) { return Message.success(request, null); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
