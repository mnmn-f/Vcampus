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
            JTextArea input = editableTextArea(holder[0]); input.setText("测试重试");
            input.getActionMap().get("send-message").actionPerformed(
                    new ActionEvent(input, ActionEvent.ACTION_PERFORMED, "send"));
            assertFalse(firstList(holder[0]).isEnabled());
        });
        service.listener.onFailure("连接中断"); flush();
        SwingUtilities.invokeAndWait(() -> button(holder[0], "重试").doClick());
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
    private static void flush() throws Exception { SwingUtilities.invokeAndWait(() -> { }); }

    private static final class FakeService implements AiAssistantClientService {
        private AiStreamListener listener; private final List<String> requestIds = new ArrayList<String>();
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
