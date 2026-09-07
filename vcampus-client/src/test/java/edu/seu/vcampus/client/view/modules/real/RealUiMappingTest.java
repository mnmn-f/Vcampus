package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionRequest;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 稳定 code 的中文呈现、提交值和实时编辑器选项回归。 */
public final class RealUiMappingTest {
    @Test
    public void newStableCodesHaveChineseLabels() {
        assertEquals("教学", RealUi.status("TEACHING"));
        assertEquals("实验", RealUi.status("LAB"));
        assertEquals("会议", RealUi.status("MEETING"));
        assertEquals("其他", RealUi.status("OTHER"));
        assertEquals("通过", RealUi.status("PASS"));
        assertEquals("不通过", RealUi.status("FAIL"));
        assertEquals("公共", RealUi.status("PUBLIC"));
        assertEquals("实践", RealUi.status("PRACTICE"));
        assertEquals("已退选", RealUi.status("DROPPED"));
        assertEquals("已结束", RealUi.status("ENDED"));
        assertEquals("遗失", RealUi.status("LOST"));
        assertEquals("未到场", RealUi.status("NO_SHOW"));
        assertEquals("消费", RealUi.status("PURCHASE"));
        assertEquals("退款", RealUi.status("REFUND"));
        assertEquals("调账", RealUi.status("ADJUSTMENT"));
    }

    @Test
    public void storeConsumptionFilterUsesDatabaseCode() throws Exception {
        Method type = StoreAccountPanel.class.getDeclaredMethod("type", String.class);
        type.setAccessible(true);
        assertEquals("PURCHASE", type.invoke(null, "消费"));
        assertEquals("REFUND", type.invoke(null, "退款"));
    }

    @Test
    public void hygieneResultEditorUsesLabelsButKeepsCode() throws Exception {
        AtomicReference<HygieneInspectionRequest> saved = new AtomicReference<HygieneInspectionRequest>();
        CountDownLatch completed = new CountDownLatch(1);
        DormManagerGovernancePanel panel = new DormManagerGovernancePanel(page(), dormService(saved, completed, null, null));
        JComboBox<?> result = field(panel, "result");
        assertEquals("通过", String.valueOf(result.getItemAt(0)));
        assertEquals("不通过", String.valueOf(result.getItemAt(1)));
        assertEquals("PASS", RealUi.code(result.getItemAt(0)));
        assertEquals("FAIL", RealUi.code(result.getItemAt(1)));
        JTextField room = field(panel, "room");
        JTextField score = field(panel, "score");
        room.setText("12");
        score.setText("95");
        result.setSelectedIndex(1);
        invoke(panel, "saveHygiene");
        assertTrue(completed.await(2, TimeUnit.SECONDS));
        assertEquals("FAIL", saved.get().getResult());
    }

    @Test
    public void dormAnnouncementScopeEditorUsesChineseOptions() throws Exception {
        AtomicReference<AnnouncementSaveRequest> saved = new AtomicReference<AnnouncementSaveRequest>();
        CountDownLatch completed = new CountDownLatch(1);
        DormAnnouncementsPanel panel = new DormAnnouncementsPanel(page(), dormService(null, null, saved, completed), Role.DORM_MANAGER);
        JComboBox<?> scope = field(panel, "scope");
        assertEquals("不限", String.valueOf(scope.getItemAt(0)));
        assertEquals("指定角色", String.valueOf(scope.getItemAt(1)));
        assertEquals("ALL", RealUi.code(scope.getItemAt(0)));
        assertEquals("ROLE", RealUi.code(scope.getItemAt(1)));
        assertFalse(scope.getItemAt(0) instanceof String);
        assertTrue(scope.getItemAt(1) instanceof RealUi.CodeOption);
        JTextField title = field(panel, "title");
        JTextArea content = field(panel, "content");
        title.setText("宿舍通知");
        content.setText("请查看通知内容。");
        scope.setSelectedIndex(1);
        invoke(panel, "save");
        assertTrue(completed.await(2, TimeUnit.SECONDS));
        assertEquals("ROLE", saved.get().getVisibleScope());
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) throws Exception {
        Field value = target.getClass().getDeclaredField(name);
        value.setAccessible(true);
        return (T) value.get(target);
    }

    private static DormClientService dormService(final AtomicReference<HygieneInspectionRequest> hygiene,
                                                  final CountDownLatch hygieneDone,
                                                  final AtomicReference<AnnouncementSaveRequest> announcement,
                                                  final CountDownLatch announcementDone) {
        return (DormClientService) Proxy.newProxyInstance(DormClientService.class.getClassLoader(),
                new Class<?>[]{DormClientService.class}, new java.lang.reflect.InvocationHandler() {
                    @Override public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("saveHygiene".equals(method.getName()) && hygiene != null) {
                        hygiene.set((HygieneInspectionRequest) args[0]); hygieneDone.countDown(); return null;
                    }
                    if ("saveAnnouncement".equals(method.getName()) && announcement != null) {
                        announcement.set((AnnouncementSaveRequest) args[0]); announcementDone.countDown(); return null;
                    }
                    return method.getReturnType() == DormPage.class
                            ? new DormPage<Object>(1, 20, 1, Collections.emptyList()) : null;
                    }
                });
    }

    private static void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static BasePage page() {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "qa", "测试", Role.DORM_MANAGER, "token"));
        return new TestPage(session);
    }

    private static final class TestPage extends BasePage {
        private TestPage(ClientSession session) { super(session, "测试", "测试页面"); }
    }
}
