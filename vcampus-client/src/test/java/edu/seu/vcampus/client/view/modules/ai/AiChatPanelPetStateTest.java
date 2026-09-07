package edu.seu.vcampus.client.view.modules.ai;

import edu.seu.vcampus.client.auth.AiAssistantClientService;
import edu.seu.vcampus.client.view.pet.PetActivityListener;
import edu.seu.vcampus.client.view.pet.PetMood;
import edu.seu.vcampus.common.ai.AiChatMessage;
import edu.seu.vcampus.common.ai.AiConfirmResult;
import edu.seu.vcampus.common.ai.AiKnowledgeChunk;
import edu.seu.vcampus.common.ai.AiKnowledgeQuery;
import edu.seu.vcampus.common.ai.AiKnowledgeSaveRequest;
import edu.seu.vcampus.common.ai.AiMode;
import edu.seu.vcampus.common.ai.AiMonitorSnapshot;
import edu.seu.vcampus.common.ai.AiPage;
import edu.seu.vcampus.common.ai.AiSessionSummary;
import edu.seu.vcampus.common.ai.AiStreamListener;
import org.junit.Test;

import javax.swing.Action;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class AiChatPanelPetStateTest {
    @Test
    public void queryLifecycleUpdatesPetWithoutChangingComposer() throws Exception {
        final FakeService service = new FakeService();
        final Recorder recorder = new Recorder();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                AiChatPanel panel = new AiChatPanel(service, recorder);
                JTextArea input = editableTextArea(panel);
                input.setText("图书馆有什么书");
                Action send = input.getActionMap().get("send-message");
                send.actionPerformed(new ActionEvent(input,
                        ActionEvent.ACTION_PERFORMED, "send-message"));
            }
        });
        assertEquals(PetMood.THINKING, recorder.mood);

        service.listener.onChunk("有《软件工程》。");
        flushEdt();
        assertEquals(PetMood.TALKING, recorder.mood);

        service.listener.onComplete();
        flushEdt();
        assertEquals(PetMood.SUCCESS, recorder.mood);
    }

    private static JTextArea editableTextArea(Component component) {
        if (component instanceof JTextArea && ((JTextArea) component).isEditable()) {
            return (JTextArea) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                JTextArea found = editableTextAreaOrNull(child);
                if (found != null) return found;
            }
        }
        throw new AssertionError("未找到消息输入框");
    }

    private static JTextArea editableTextAreaOrNull(Component component) {
        if (component instanceof JTextArea && ((JTextArea) component).isEditable()) {
            return (JTextArea) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                JTextArea found = editableTextAreaOrNull(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() { }
        });
    }

    private static final class Recorder implements PetActivityListener {
        private volatile PetMood mood;

        @Override
        public void onMood(PetMood value, String bubble, long durationMillis) {
            mood = value;
        }
    }

    private static final class FakeService implements AiAssistantClientService {
        private volatile AiStreamListener listener;

        @Override
        public String query(String sessionId, String text, AiMode mode,
                            AiStreamListener value) {
            listener = value;
            return "request-1";
        }

        @Override public void cancel(String requestId) { }
        @Override public List<AiSessionSummary> sessions() {
            return Collections.emptyList();
        }
        @Override public String createSession() { return "session-1"; }
        @Override public List<AiChatMessage> history(String sessionId) {
            return Collections.emptyList();
        }
        @Override public void clearSession(String sessionId) { }
        @Override public AiConfirmResult confirm(long actionId, boolean agreed) { return null; }
        @Override public AiPage<AiKnowledgeChunk> knowledge(AiKnowledgeQuery query) { return null; }
        @Override public AiKnowledgeChunk saveKnowledge(AiKnowledgeSaveRequest request) {
            return null;
        }
        @Override public void deleteKnowledge(long chunkId) { }
        @Override public AiMonitorSnapshot monitor() { return null; }
    }
}
