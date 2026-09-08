package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.common.ai.*;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class AiChatPanelRetryTest {
    @Test public void retryReusesLogicalRequestAndDoesNotAppendAnotherUserBubble() throws Exception {
        final FakeService service = new FakeService();
        final AiChatPanel[] holder = new AiChatPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new AiChatPanel(service);
        });
        awaitListEnabled(holder[0]);
        SwingUtilities.invokeAndWait(() -> {
            JTextArea input = editableTextArea(holder[0]); input.setText("测试重试");
            input.getActionMap().get("send-message").actionPerformed(
                    new ActionEvent(input, ActionEvent.ACTION_PERFORMED, "send"));
            assertFalse(firstList(holder[0]).isEnabled());
        });
        SwingUtilities.invokeAndWait(() -> service.listener.onFailure("连接中断"));
        awaitEnabled(holder[0], "重试");
        SwingUtilities.invokeAndWait(() -> enabledButton(holder[0], "重试").doClick());
        awaitRequests(service, 2);
        assertEquals(2, service.requestIds.size());
        assertEquals(service.requestIds.get(0), service.requestIds.get(1));
        assertEquals(1, countCards(holder[0], "测试重试"));
    }

    private static int countCards(Component component, String text) {
        int count = component instanceof AiMessageCard
                && text.equals(((AiMessageCard) component).getText()) ? 1 : 0;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) {
            count += countCards(child, text);
        }
        return count;
    }
    private static JTextArea editableTextArea(Component component) {
        if (component instanceof JTextArea && ((JTextArea) component).isEditable()) return (JTextArea) component;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) {
            try { return editableTextArea(child); } catch (AssertionError ignored) { }
        }
        throw new AssertionError("input not found");
    }
    private static JList<?> firstList(Component component) {
        if (component instanceof JList) return (JList<?>) component;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) {
            try { return firstList(child); } catch (AssertionError ignored) { }
        }
        throw new AssertionError("list not found");
    }
    private static JButton button(Component component, String text) {
        if (component instanceof JButton && text.equals(((JButton) component).getText())) return (JButton) component;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) {
            try { return button(child, text); } catch (AssertionError ignored) { }
        }
        throw new AssertionError("button not found");
    }
    private static JButton enabledButton(Component component, String text) {
        if (component instanceof JButton && ((JButton) component).isEnabled()
                && text.equals(((JButton) component).getText())) return (JButton) component;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) {
            try { return enabledButton(child, text); } catch (AssertionError ignored) { }
        }
        throw new AssertionError("enabled button not found");
    }
    private static void flush() throws Exception { SwingUtilities.invokeAndWait(() -> { }); }

    private static void awaitEnabled(Component component, String text) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (System.currentTimeMillis() < deadline) {
            flush();
            try { enabledButton(component, text); return; }
            catch (AssertionError ignored) { Thread.sleep(10L); }
        }
        enabledButton(component, text);
    }

    private static void awaitRequests(FakeService service, int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (service.requestIds.size() < expected && System.currentTimeMillis() < deadline) {
            flush(); Thread.sleep(10L);
        }
    }

    private static void awaitListEnabled(Component component) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (!firstList(component).isEnabled() && System.currentTimeMillis() < deadline) {
            flush(); Thread.sleep(10L);
        }
        if (!firstList(component).isEnabled()) throw new AssertionError("session list did not finish loading");
    }

    private static final class FakeService implements AiAssistantClientService {
        private AiStreamListener listener;
        private final List<String> requestIds = Collections.synchronizedList(new ArrayList<String>());
        public String query(String sessionId, String text, AiMode mode, AiStreamListener listener) {
            this.listener = listener; return "fallback";
        }
        public String queryWithRequestId(String requestId, String sessionId, String text, AiMode mode,
                List<AiAttachment> attachments, AiStreamListener listener) {
            requestIds.add(requestId); this.listener = listener; return requestId;
        }
        public void cancel(String requestId) { }
        public List<AiSessionSummary> sessions() { return Collections.emptyList(); }
        public String createSession() { return "1"; }
        public List<AiChatMessage> history(String sessionId) { return Collections.emptyList(); }
        public void clearSession(String sessionId) { }
        public AiConfirmResult confirm(long actionId, boolean agreed) { return null; }
        public AiPage<AiKnowledgeChunk> knowledge(AiKnowledgeQuery query) { return null; }
        public AiKnowledgeChunk saveKnowledge(AiKnowledgeSaveRequest request) { return null; }
        public void deleteKnowledge(long chunkId) { }
        public AiMonitorSnapshot monitor() { return null; }
    }
}
