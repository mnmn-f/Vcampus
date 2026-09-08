package edu.seu.vcampus.client.view.modules;

import edu.seu.vcampus.client.auth.DisabledAiAssistantClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.view.modules.ai.AiChatPanel;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** 验证聊天页固定输入区，不再嵌套到整页滚动容器中。 */
public final class AiAssistantPageLayoutTest {
    @Test
    public void studentChatKeepsComposerInsideFixedViewport() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                ClientSession session = new ClientSession();
                session.open(new LoginResult(10L, "student", "学生", Role.STUDENT, "token"));
                AiAssistantPage page = AiAssistantPage.create(session,
                        () -> new DisabledAiAssistantClientService());
                page.setSize(900, 600);
                layout(page);

                Component center = ((BorderLayout) page.getLayout())
                        .getLayoutComponent(BorderLayout.CENTER);
                assertTrue(center instanceof AiChatPanel);
                assertSame(center, find(page, AiChatPanel.class));
                assertFalse(hasDirectChild(page, JScrollPane.class));

                JTextArea input = findEditableTextArea(page);
                JButton send = findButton(page, "发送");
                assertNotNull(input);
                assertNotNull(send);
                assertInside(page, input);
                assertInside(page, send);
                assertNotNull(findButton(page, "展开会话侧栏"));
                assertFalse(hasText(page, "校园助手对话"));
            }
        });
    }

    @Test
    public void adminWorkbenchKeepsAllSevenTasksInScrollableViewports() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                ClientSession session = new ClientSession();
                session.open(new LoginResult(20L, "ai-admin", "知识管理员",
                        Role.AI_KNOWLEDGE_ADMIN, "token"));
                AiAssistantPage page = AiAssistantPage.create(session,
                        () -> new DisabledAiAssistantClientService());
                page.setSize(760, 520);
                layout(page);

                Component center = ((BorderLayout) page.getLayout())
                        .getLayoutComponent(BorderLayout.CENTER);
                assertTrue(center instanceof JTabbedPane);
                JTabbedPane tabs = (JTabbedPane) center;
                assertTrue(tabs.getTabCount() == 7);
                for (int index = 0; index < tabs.getTabCount(); index++) {
                    assertTrue(tabs.getComponentAt(index) instanceof JScrollPane);
                    JScrollPane scroll = (JScrollPane) tabs.getComponentAt(index);
                    assertTrue(scroll.getViewport().getView() instanceof javax.swing.Scrollable);
                    assertTrue(((javax.swing.Scrollable) scroll.getViewport().getView())
                            .getScrollableTracksViewportWidth());
                }
            }
        });
    }

    private static void assertInside(Container root, Component child) {
        Rectangle bounds = SwingUtilities.convertRectangle(child.getParent(),
                child.getBounds(), root);
        assertTrue("component must be visible", bounds.width > 0 && bounds.height > 0);
        assertTrue("component must stay inside page", bounds.y >= 0
                && bounds.y + bounds.height <= root.getHeight());
    }

    private static boolean hasDirectChild(Container root, Class<?> type) {
        for (Component child : root.getComponents()) if (type.isInstance(child)) return true;
        return false;
    }

    private static JTextArea findEditableTextArea(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTextArea && ((JTextArea) child).isEditable()) {
                return (JTextArea) child;
            }
            if (child instanceof Container) {
                JTextArea found = findEditableTextArea((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JButton findButton(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton) child).getText())) {
                return (JButton) child;
            }
            if (child instanceof Container) {
                JButton found = findButton((Container) child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasText(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof javax.swing.JLabel
                    && text.equals(((javax.swing.JLabel) child).getText())) return true;
            if (child instanceof Container && hasText((Container) child, text)) return true;
        }
        return false;
    }

    private static <T> T find(Container root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        for (Component child : root.getComponents()) if (child instanceof Container) {
            T found = find((Container) child, type);
            if (found != null) return found;
        }
        return null;
    }

    private static void layout(Component root) {
        root.doLayout();
        if (root instanceof Container) for (Component child : ((Container) root).getComponents()) {
            layout(child);
        }
    }
}
