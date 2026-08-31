package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 图书馆复用通用 Campus 公告，不复制图书馆专用服务边界。 */
public final class CampusLibraryAnnouncementsTest {
    @Test public void librarianSaveUsesLibraryModuleCode() throws Exception {
        final AtomicReference<CampusAnnouncementSaveRequest> saved = new AtomicReference<CampusAnnouncementSaveRequest>();
        final CampusAnnouncementEditorPanel editor = editor(saved);
        click(editor, "保存公告");
        assertNotNull(saved.get());
        assertEquals("LIBRARY", saved.get().getModuleCode());
        assertEquals("ALL", saved.get().getVisibleScope());
        assertNull(saved.get().getTargetRoleCode());
    }

    @Test public void scopeAndTargetRoleUseChineseOptions() throws Exception {
        final AtomicReference<CampusAnnouncementSaveRequest> saved = new AtomicReference<CampusAnnouncementSaveRequest>();
        final CampusAnnouncementEditorPanel editor = editor(saved);
        assertFalse(hasLabel(editor, "可见范围（ALL/ROLE）"));
        assertFalse(hasLabel(editor, "目标角色编码（可选）"));
        JComboBox<?> scope = comboWith(editor, "全部用户");
        JComboBox<?> target = comboWith(editor, "学生");
        assertNotNull(scope); assertNotNull(target); assertFalse(target.isEnabled());
        scope.setSelectedItem(itemWith(scope, "指定角色"));
        assertTrue(target.isEnabled()); target.setSelectedItem(itemWith(target, "学生"));
        click(editor, "保存公告");
        assertEquals("ROLE", saved.get().getVisibleScope());
        assertEquals("STUDENT", saved.get().getTargetRoleCode());
    }

    @Test public void onlyLibrarianSeesLibraryAnnouncementActions() {
        BasePage page = page(Role.STUDENT);
        assertFalse(has(new CampusAnnouncementsPanel(page, service(), Role.STUDENT,
                "LIBRARY", "图书馆公告", Role.LIBRARIAN), "新建公告"));
        assertTrue(has(new CampusAnnouncementsPanel(page(Role.LIBRARIAN), service(), Role.LIBRARIAN,
                "LIBRARY", "图书馆公告", Role.LIBRARIAN), "新建公告"));
    }

    private static CampusClientService service() {
        return (CampusClientService) Proxy.newProxyInstance(
                CampusClientService.class.getClassLoader(), new Class<?>[]{CampusClientService.class},
                new java.lang.reflect.InvocationHandler() {
                    @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        return method.getReturnType() == CampusPage.class ? new CampusPage<Object>(1, 20, 0, Collections.emptyList()) : null;
                    }
                });
    }

    private static BasePage page(Role role) {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "qa", "测试", role, "token"));
        return new TestPage(session);
    }

    private static void click(Component root, String label) throws Exception {
        final JButton button = find(root, label);
        assertNotNull(button);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() { button.doClick(); }
        });
    }

    private static boolean has(Component root, String label) { return find(root, label) != null; }

    private static CampusAnnouncementEditorPanel editor(final AtomicReference<CampusAnnouncementSaveRequest> saved) {
        CampusAnnouncementEditorPanel value = new CampusAnnouncementEditorPanel(
                "library", "图书馆公告编辑", new CampusAnnouncementEditorPanel.Listener() {
                    @Override public void onSave(CampusAnnouncementSaveRequest request) { saved.set(request); }
                });
        List<JTextField> fields = new ArrayList<JTextField>(); collect(value, JTextField.class, fields);
        fields.get(0).setText("开放时间调整");
        List<JTextArea> areas = new ArrayList<JTextArea>(); collect(value, JTextArea.class, areas);
        areas.get(0).setText("本周末图书馆延长开放时间。");
        return value;
    }

    private static boolean hasLabel(Component root, String text) {
        if (root instanceof JLabel && text.equals(((JLabel) root).getText())) return true;
        if (!(root instanceof Container)) return false;
        for (Component child : ((Container) root).getComponents()) if (hasLabel(child, text)) return true;
        return false;
    }

    private static JComboBox<?> comboWith(Component root, String text) {
        if (root instanceof JComboBox && itemWith((JComboBox<?>) root, text) != null) return (JComboBox<?>) root;
        if (!(root instanceof Container)) return null;
        for (Component child : ((Container) root).getComponents()) {
            JComboBox<?> value = comboWith(child, text); if (value != null) return value;
        }
        return null;
    }

    private static Object itemWith(JComboBox<?> box, String text) {
        for (int i = 0; i < box.getItemCount(); i++) {
            Object value = box.getItemAt(i); if (text.equals(String.valueOf(value))) return value;
        }
        return null;
    }
    private static JButton find(Component root, String label) {
        if (root instanceof JButton && label.equals(((JButton) root).getText())) return (JButton) root;
        if (!(root instanceof Container)) return null;
        for (Component child : ((Container) root).getComponents()) { JButton value = find(child, label); if (value != null) return value; }
        return null;
    }

    private static <T> void collect(Component root, Class<T> type, List<T> result) {
        if (type.isInstance(root)) result.add(type.cast(root));
        if (!(root instanceof Container)) return;
        for (Component child : ((Container) root).getComponents()) collect(child, type, result);
    }

    private static final class TestPage extends BasePage {
        private TestPage(ClientSession session) { super(session, "测试", "测试页面"); }
    }
}
