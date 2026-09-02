package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.client.view.PersonalCenterPage;
import edu.seu.vcampus.client.view.modules.ModulePages;
import edu.seu.vcampus.common.module.ModuleId;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 角色页面组合只暴露当前职责的校园扩展和系统操作。 */
public final class ClientRoleCompositionTest {
    @Test public void studentGetsEnrollmentAndAdminGetsMaintenanceActions() {
        ClientBusinessServices services = services(Role.STUDENT);
        BasePage student = ModulePages.forModule(ModuleId.ACADEMIC, session(Role.STUDENT),
                null, services);
        assertTrue(has(student, "报名")); assertTrue(has(student, "提交申请"));

        services = services(Role.SYSTEM_ADMIN);
        BasePage admin = ModulePages.forModule(ModuleId.USER_ADMIN, session(Role.SYSTEM_ADMIN),
                null, services);
        assertTrue(has(admin, "保存启停状态")); assertTrue(has(admin, "强制下线"));
    }

    @Test public void scheduleMaintenanceIsOnlyComposedForAcademicAdmin() {
        BasePage admin = ModulePages.forModule(ModuleId.ACADEMIC, session(Role.ACADEMIC_ADMIN),
                null, services(Role.ACADEMIC_ADMIN));
        assertTrue(hasType(admin, CourseScheduleEditorPanel.class));
        assertTrue(has(admin, "保存时段")); assertTrue(has(admin, "删除时段"));
        BasePage teacher = ModulePages.forModule(ModuleId.ACADEMIC, session(Role.TEACHER),
                null, services(Role.TEACHER));
        BasePage student = ModulePages.forModule(ModuleId.ACADEMIC, session(Role.STUDENT),
                null, services(Role.STUDENT));
        assertFalse(hasType(teacher, CourseScheduleEditorPanel.class));
        assertFalse(hasType(student, CourseScheduleEditorPanel.class));
    }

    @Test public void libraryNavigationMatchesStudentAndLibrarianDesign() {
        BasePage student = ModulePages.forModule(ModuleId.LIBRARY, session(Role.STUDENT),
                null, services(Role.STUDENT));
        assertTrue(has(student, "首页"));
        assertTrue(has(student, "图书查阅"));
        assertTrue(has(student, "自习室预约"));
        assertTrue(has(student, "线上资源"));
        assertFalse(has(student, "图书管理"));

        BasePage librarian = ModulePages.forModule(ModuleId.LIBRARY, session(Role.LIBRARIAN),
                null, services(Role.LIBRARIAN));
        assertTrue(has(librarian, "公告管理"));
        assertTrue(has(librarian, "图书管理"));
        assertTrue(has(librarian, "借阅管理"));
        assertTrue(has(librarian, "自习室管理"));
        assertTrue(has(librarian, "线上资源管理"));

        BasePage teacher = ModulePages.forModule(ModuleId.LIBRARY, session(Role.TEACHER),
                null, services(Role.TEACHER));
        assertFalse(hasType(teacher, LibraryRoomEditorPanel.class));
    }

    @Test public void personalCenterComposesCancellationOnlyOnceForOrdinaryUsers() {
        PersonalCenterPage student = new PersonalCenterPage(session(Role.STUDENT),
                services(Role.STUDENT), null);
        assertEquals(1, countType(student, IdentityCancellationPanel.class));

        PersonalCenterPage admin = new PersonalCenterPage(session(Role.SYSTEM_ADMIN),
                services(Role.SYSTEM_ADMIN), null);
        assertEquals(0, countType(admin, IdentityCancellationPanel.class));
    }

    private static ClientBusinessServices services(Role role) {
        return new ClientBusinessServices(new NetworkClientService(new EmptyGateway()), session(role));
    }
    private static ClientSession session(Role role) {
        ClientSession value = new ClientSession(); value.open(new LoginResult(7L, "qa", "测试", role, "token-7")); return value;
    }
    private static boolean has(Component root, String text) {
        if (root instanceof JButton && text.equals(((JButton) root).getText())) return true;
        if (!(root instanceof Container)) return false;
        for (Component child : ((Container) root).getComponents()) if (has(child, text)) return true;
        return false;
    }
    private static boolean hasType(Component root, Class<?> type) {
        if (type.isInstance(root)) return true;
        if (!(root instanceof Container)) return false;
        for (Component child : ((Container) root).getComponents()) if (hasType(child, type)) return true;
        return false;
    }
    private static int countType(Component root, Class<?> type) {
        int count = type.isInstance(root) ? 1 : 0;
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                count += countType(child, type);
            }
        }
        return count;
    }
    private static final class EmptyGateway implements ClientGateway {
        @Override public Message send(Message request) { return Message.success(request, null); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
