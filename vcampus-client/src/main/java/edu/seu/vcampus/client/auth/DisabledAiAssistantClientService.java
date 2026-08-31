package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.ai.AiStreamListener;

import java.util.UUID;

/** 未接入模型时的明确降级实现，不伪造 AI 回答。 */
public final class DisabledAiAssistantClientService implements AiAssistantClientService {
    @Override
    public void query(String sessionId, String text, AiStreamListener listener) {
        if (listener == null) {
            return;
        }
        listener.onFailure("校园助手暂不可用。");
    }

    @Override
    public void cancel(String requestId) {
        // 接口阶段没有后台模型任务。
    }

    public String newRequestId() {
        return UUID.randomUUID().toString();
    }
}
