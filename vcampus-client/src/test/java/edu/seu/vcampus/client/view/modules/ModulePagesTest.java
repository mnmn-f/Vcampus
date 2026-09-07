package edu.seu.vcampus.client.view.modules;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.modules.real.RealAcademicPage;
import edu.seu.vcampus.client.view.modules.real.RealDormPage;
import edu.seu.vcampus.client.view.modules.real.RealIdentityAdminPage;
import edu.seu.vcampus.client.view.modules.real.RealLibraryPage;
import edu.seu.vcampus.client.view.modules.real.RealStorePage;
import edu.seu.vcampus.client.view.modules.real.RealStudentRecordPage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 页面工厂在 Demo 模式接入可操作图书馆，其他未连接模块仍显示占位页。 */
public final class ModulePagesTest {
    @Test
    public void demoModulesShareOneShortPlaceholderWithoutActionsOrRecords() {
        ClientSession session = session(Role.STUDENT);
        List<ModuleId> modules = Arrays.asList(ModuleId.STUDENT_RECORD, ModuleId.ACADEMIC,
                ModuleId.LIBRARY, ModuleId.STORE, ModuleId.DORMITORY,
                ModuleId.USER_ADMIN, ModuleId.SYSTEM, ModuleId.AI_ASSISTANT);
        for (ModuleId module : modules) {
            BasePage page = ModulePages.forModule(module, session, null);
            assertTrue(module + " 未使用统一占位页", page instanceof ServiceRequiredPage);
            String text = visibleText(page);
            assertEquals(module.toString(), 1, occurrences(text, "需要连接服务端"));
            assertFalse(text.contains("CS101"));
            assertFalse(text.contains("student01"));
            assertFalse(text.contains("¥39.00"));
            assertFalse(hasActionButton(page));
        }
    }

    @Test
    public void demoModeCreatesOperableLibraryPage() {
        String old = System.getProperty("vcampus.client.mode");
        try {
            System.setProperty("vcampus.client.mode", "demo");
            BasePage page = ModulePages.forModule(ModuleId.LIBRARY,
                    session(Role.STUDENT), null);
            assertTrue(page instanceof RealLibraryPage);
            String text = visibleText(page);
            assertTrue(text.contains("图书查阅"));
            assertTrue(text.contains("自习室预约"));
            assertTrue(text.contains("确认预约"));
        } finally {
            if (old == null) System.clearProperty("vcampus.client.mode");
            else System.setProperty("vcampus.client.mode", old);
        }
    }

    @Test
    public void networkModulesAreCreatedFromRealPages() {
        assertTrue(page(ModuleId.STUDENT_RECORD, Role.STUDENT) instanceof RealStudentRecordPage);
        assertTrue(page(ModuleId.ACADEMIC, Role.ACADEMIC_ADMIN) instanceof RealAcademicPage);
        assertTrue(page(ModuleId.LIBRARY, Role.LIBRARIAN) instanceof RealLibraryPage);
        assertTrue(page(ModuleId.STORE, Role.STORE_MANAGER) instanceof RealStorePage);
        assertTrue(page(ModuleId.DORMITORY, Role.DORM_MANAGER) instanceof RealDormPage);
        assertTrue(page(ModuleId.USER_ADMIN, Role.SYSTEM_ADMIN) instanceof RealIdentityAdminPage);
        assertTrue(page(ModuleId.SYSTEM, Role.SYSTEM_ADMIN) instanceof RealIdentityAdminPage);
    }

    private static BasePage page(ModuleId module, Role role) {
        ClientSession session = session(role);
        ClientBusinessServices services = new ClientBusinessServices(
                new NetworkClientService(new EmptyGateway()), session);
        return ModulePages.forModule(module, session, null, services);
    }

    private static ClientSession session(Role role) {
        ClientSession value = new ClientSession();
        value.open(new LoginResult(7L, "qa", "测试用户", role, "token-7"));
        return value;
    }

    private static String visibleText(Component root) {
        StringBuilder text = new StringBuilder();
        collect(root, text);
        return text.toString();
    }

    private static void collect(Component root, StringBuilder text) {
        if (root instanceof JLabel) text.append(((JLabel) root).getText()).append('\n');
        if (root instanceof AbstractButton) text.append(((AbstractButton) root).getText()).append('\n');
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) collect(child, text);
        }
    }

    private static boolean hasActionButton(Component root) {
        if (root instanceof AbstractButton) {
            String label = ((AbstractButton) root).getText();
            if (label != null && label.trim().length() > 0) return true;
        }
        if (!(root instanceof Container)) return false;
        for (Component child : ((Container) root).getComponents()) if (hasActionButton(child)) return true;
        return false;
    }

    private static int occurrences(String text, String value) {
        int count = 0;
        for (int start = 0; (start = text.indexOf(value, start)) >= 0; start += value.length()) count++;
        return count;
    }

    private static final class EmptyGateway implements ClientGateway {
        @Override public Message send(Message request) { return Message.success(request, null); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
