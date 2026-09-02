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
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 宿舍页面的标签结构。
 *
 * <p>两件事值得钉住：一是拆开 main 的「日常治理」靠的是它内部「表格—动作—表格—动作」
 * 的结构，main 改了结构这里要能立刻发现；二是各标签页高度差很大，容器必须按当前页
 * 伸缩，否则矮的页面底下会拖出一大片空白。</p>
 */
public final class DormExtTabLayoutTest {
    @Test
    public void managerTabsAreOrganisedByTask() {
        assertEquals(Arrays.asList("住宿与空间", "申请与审批", "报修处理", "水电",
                "未归管理", "卫生管理", "宿舍公告", "设置"), titles(managerPage()));
    }

    @Test
    public void studentTabsAreOrganisedByTask() {
        assertEquals(Arrays.asList("我的住宿", "申请与登记", "报修服务", "水电账单",
                "在宿门禁", "宿舍公告"), titles(studentPage()));
    }

    @Test
    public void governancePanelIsSplitIntoAbsenceAndHygiene() {
        BasePage page = managerPage();
        JTabbedPane tabs = tabs(page);
        Component absence = tabs.getComponentAt(indexOf(tabs, "未归管理"));
        Component hygiene = tabs.getComponentAt(indexOf(tabs, "卫生管理"));

        // main 的两块各自落到对应的一页，没有互相串门
        assertTrue("未归页应当带着 main 的未归预警列表", hasText(absence, "未归预警"));
        assertTrue("以及它的处理动作", hasText(absence, "处理未归"));
        assertFalse("卫生的东西不该出现在未归页", hasText(absence, "卫生检查与整改"));

        assertTrue("卫生页应当带着 main 的卫生检查列表", hasText(hygiene, "卫生检查"));
        assertTrue("以及它的检查表单", hasText(hygiene, "卫生检查与整改"));
        assertFalse("未归的东西不该出现在卫生页", hasText(hygiene, "处理未归"));
    }

    @Test
    public void bothHalvesKeepTheirOwnExtensionPanels() {
        JTabbedPane tabs = tabs(managerPage());
        assertTrue(hasText(tabs.getComponentAt(indexOf(tabs, "未归管理")), "手动扫描"));
        assertTrue(hasText(tabs.getComponentAt(indexOf(tabs, "未归管理")), "在宿一览"));
        assertTrue(hasText(tabs.getComponentAt(indexOf(tabs, "卫生管理")), "卫生检查评分"));
    }

    @Test
    public void settingsCollectsTheRarelyUsedControls() {
        JTabbedPane tabs = tabs(managerPage());
        Component settings = tabs.getComponentAt(indexOf(tabs, "设置"));
        assertTrue("运行状态放在设置页，管理员不必跑到服务器跟前看控制台",
                hasText(settings, "服务端运行状态"));
        assertTrue(hasText(settings, "门禁时段"));
        assertTrue(hasText(settings, "未归预警阈值"));
        assertTrue(hasText(settings, "删除房间"));
    }

    @Test
    public void onlyTheSelectedTabContributesItsHeight() {
        // 没显示的页首选高度被压成 0，否则最高的一页会把每一页都撑高，
        // 矮的页面底下就会多出一大片可以往下滚的空白。
        JTabbedPane tabs = tabs(managerPage());
        int selected = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            int height = tabs.getComponentAt(i).getPreferredSize().height;
            if (i == selected) {
                assertTrue("当前页要报出真实高度", height > 0);
            } else {
                assertEquals("未显示的页不应把容器撑高", 0, height);
            }
        }
    }

    @Test
    public void switchingTabsMovesTheHeightWithIt() {
        JTabbedPane tabs = tabs(managerPage());
        tabs.setSelectedIndex(tabs.getTabCount() - 1);
        assertEquals(0, tabs.getComponentAt(0).getPreferredSize().height);
        assertTrue(tabs.getComponentAt(tabs.getTabCount() - 1).getPreferredSize().height > 0);
    }

    // ---------- 脚手架 ----------

    private static BasePage managerPage() {
        return new RealDormPage(session(Role.DORM_MANAGER), services());
    }

    private static BasePage studentPage() {
        return new RealDormPage(session(Role.STUDENT), services());
    }

    private static int indexOf(JTabbedPane tabs, String title) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (title.equals(tabs.getTitleAt(i))) return i;
        }
        throw new AssertionError("没有名为 " + title + " 的标签页");
    }

    private static List<String> titles(Component page) {
        JTabbedPane tabs = tabs(page);
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < tabs.getTabCount(); i++) names.add(tabs.getTitleAt(i));
        return names;
    }

    private static JTabbedPane tabs(Component root) {
        if (root instanceof JTabbedPane) return (JTabbedPane) root;
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                JTabbedPane found = tabs(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasText(Component root, String text) {
        if (root instanceof JLabel && text.equals(((JLabel) root).getText())) return true;
        if (root instanceof AbstractButton && text.equals(((AbstractButton) root).getText())) return true;
        if (!(root instanceof Container)) return false;
        for (Component child : ((Container) root).getComponents()) {
            if (hasText(child, text)) return true;
        }
        return false;
    }

    private static ClientBusinessServices services() {
        return new ClientBusinessServices(new NetworkClientService(new EmptyGateway()),
                session(Role.STUDENT));
    }

    private static ClientSession session(Role role) {
        ClientSession value = new ClientSession();
        value.open(new LoginResult(7L, "qa", "测试", role, "token-7"));
        return value;
    }

    private static final class EmptyGateway implements ClientGateway {
        @Override public Message send(Message request) { return Message.success(request, null); }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
