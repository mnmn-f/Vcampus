package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.ai.AiStreamListener;

/** AI 客户端边界；当前只保留接口阶段的能力占位。 */
public interface AiAssistantClientService {
    void query(String sessionId, String text, AiStreamListener listener);

    void cancel(String requestId);
}
