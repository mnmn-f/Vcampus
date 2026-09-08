package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JComboBox;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 资源访问只允许安全的 http/https 地址，不在测试中启动浏览器。 */
public final class LibraryResourceAccessPanelTest {
    @Test public void onlyHttpAndHttpsUrisAreAccepted() {
        assertTrue(LibraryResourcesPanel.isSafeWebUrl("https://example.com/library"));
        assertTrue(LibraryResourcesPanel.isSafeWebUrl("http://127.0.0.1:8080/resource"));
        assertFalse(LibraryResourcesPanel.isSafeWebUrl("file:///tmp/private.txt"));
        assertFalse(LibraryResourcesPanel.isSafeWebUrl("javascript:alert(1)"));
        assertFalse(LibraryResourcesPanel.isSafeWebUrl("https:///missing-host"));
    }

    @Test public void accessLogsAreOnlyShownToLibrarian() {
        LibraryClientService service = emptyService();
        assertFalse(hasClass(new LibraryResourcesPanel(page(Role.STUDENT), service, Role.STUDENT),
                "LibraryResourceAccessLogsPanel"));
        assertTrue(hasClass(new LibraryResourcesPanel(page(Role.LIBRARIAN), service, Role.LIBRARIAN),
                "LibraryResourceAccessLogsPanel"));
    }

    @Test public void studentFilterOnlyOffersEnabledResources() {
        LibraryResourcesPanel panel = new LibraryResourcesPanel(page(Role.STUDENT), emptyService(), Role.STUDENT);
        JComboBox<?> filter = findFilter(panel);
        assertEquals(1, filter.getItemCount());
        assertEquals("已启用", filter.getItemAt(0));
    }

    private static LibraryClientService emptyService() {
        return (LibraryClientService) Proxy.newProxyInstance(
                LibraryClientService.class.getClassLoader(), new Class<?>[]{LibraryClientService.class},
                new java.lang.reflect.InvocationHandler() {
                    @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if (method.getReturnType() == PageResult.class) return new PageResult<Object>(Collections.<Object>emptyList(), 1, 20, 0L);
                        if (method.getReturnType() == OnlineResourceAccessLogPage.class) return new OnlineResourceAccessLogPage(Collections.<edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto>emptyList(), 1, 20, 0L);
                        return null;
                    }
                });
    }

    private static BasePage page(Role role) {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "qa", "测试", role, "token"));
        return new TestPage(session);
    }

    private static boolean hasClass(Component root, String name) {
        if (root.getClass().getSimpleName().equals(name)) return true;
        if (!(root instanceof Container)) return false;
        for (Component child : ((Container) root).getComponents()) if (hasClass(child, name)) return true;
        return false;
    }

    private static JComboBox<?> findFilter(Component root) {
        if (root instanceof JComboBox) return (JComboBox<?>) root;
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            JComboBox<?> found = findFilter(child); if (found != null) return found;
        }
        return null;
    }

    private static final class TestPage extends BasePage {
        private TestPage(ClientSession session) { super(session, "测试", "测试页面"); }
    }
}
