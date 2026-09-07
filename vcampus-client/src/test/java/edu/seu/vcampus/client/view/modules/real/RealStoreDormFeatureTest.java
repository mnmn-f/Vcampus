package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.composition.ClientBusinessServices;
import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.AbstractButton;
import java.awt.Component;
import java.awt.Container;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 商店/宿舍真实页面按 activeRole 隔离销售、审批、评价和缴费动作。 */
public final class RealStoreDormFeatureTest {
    @Test
    public void storeAndDormPagesExposeOnlyCurrentRoleActions() {
        assertEquals("给排水", RealUi.status("WATER"));
        assertEquals("照明", RealUi.status("LIGHTING"));
        assertEquals("标准间", RealUi.status("STANDARD"));
        ClientBusinessServices studentServices = services();
        BasePage studentStore = new RealStorePage(session(Role.STUDENT), studentServices);
        BasePage studentDorm = new RealDormPage(session(Role.STUDENT), studentServices);
        assertFalse(hasText(studentStore, "销售统计"));
        assertTrue(hasButton(studentStore, "取消订单"));
        assertFalse(hasText(studentDorm, "空间维护"));
        assertFalse(hasText(studentDorm, "请假审批筛选"));
        assertTrue(hasText(studentDorm, "学生请假"));
        assertTrue(hasText(studentDorm, "报修评价"));
        assertTrue(hasButton(studentDorm, "缴纳选中账单"));

        ClientBusinessServices managerServices = services();
        BasePage managerStore = new RealStorePage(session(Role.STORE_MANAGER), managerServices);
        BasePage managerDorm = new RealDormPage(session(Role.DORM_MANAGER), managerServices);
        assertTrue(hasText(managerStore, "销售统计"));
        assertTrue(hasText(managerDorm, "空间维护"));
        assertTrue(hasText(managerDorm, "请假审批筛选"));
        assertFalse(hasText(managerDorm, "学生请假"));
        assertFalse(hasText(managerDorm, "报修评价"));
        assertFalse(hasButton(managerDorm, "缴纳选中账单"));
        assertFalse(hasButton(managerDorm, "提交评价"));
    }

    private static ClientBusinessServices services() {
        return new ClientBusinessServices(new NetworkClientService(new EmptyGateway()), session(Role.STUDENT));
    }

    private static ClientSession session(Role role) {
        ClientSession value = new ClientSession();
        value.open(new LoginResult(7L, "qa", "测试", role, "token-7"));
        return value;
    }

    private static boolean hasText(Component root, String text) {
        if (root instanceof javax.swing.JLabel && text.equals(((javax.swing.JLabel) root).getText())) return true;
        if (root instanceof AbstractButton && text.equals(((AbstractButton) root).getText())) return true;
        if (!(root instanceof Container)) return false;
        for (Component child : ((Container) root).getComponents()) if (hasText(child, text)) return true;
        return false;
    }

    private static boolean hasButton(Component root, String text) {
        if (root instanceof AbstractButton && text.equals(((AbstractButton) root).getText())) return true;
        if (!(root instanceof Container)) return false;
        for (Component child : ((Container) root).getComponents()) if (hasButton(child, text)) return true;
        return false;
    }

    private static final class EmptyGateway implements ClientGateway {
        @Override public Message send(Message request) { return Message.success(request, null); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
