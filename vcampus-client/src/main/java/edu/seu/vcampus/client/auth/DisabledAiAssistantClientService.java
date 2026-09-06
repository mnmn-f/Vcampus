package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.ai.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 未接入模型时的明确降级实现，不伪造 AI 回答。 */
public final class DisabledAiAssistantClientService implements AiAssistantClientService {
    @Override
    public String query(String sessionId, String text, AiMode mode, AiStreamListener listener) {
        String requestId = UUID.randomUUID().toString();
        if (listener == null) {
            return requestId;
        }
        listener.onFailure("校园助手暂不可用。");
        return requestId;
    }

    @Override
    public void cancel(String requestId) {
        // 接口阶段没有后台模型任务。
    }

    public boolean ping() { return false; }

    public String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public List<AiSessionSummary> sessions() { return Collections.emptyList(); }
    public String createSession() { return ""; }
    public List<AiChatMessage> history(String sessionId) { return Collections.emptyList(); }
    public void clearSession(String sessionId) { }
    public AiConfirmResult confirm(long actionId, boolean agreed) {
        return new AiConfirmResult(null, "校园助手暂不可用。");
    }
    public AiPage<AiKnowledgeChunk> knowledge(AiKnowledgeQuery query) {
        return new AiPage<AiKnowledgeChunk>(Collections.<AiKnowledgeChunk>emptyList(), 0, 1, 20);
    }
    public AiKnowledgeChunk saveKnowledge(AiKnowledgeSaveRequest request) { return null; }
    public void deleteKnowledge(long chunkId) { }
    public AiMonitorSnapshot monitor() {
        return new AiMonitorSnapshot(false, "未配置", 0, 0, 0, 0, 0, 0);
    }
}
