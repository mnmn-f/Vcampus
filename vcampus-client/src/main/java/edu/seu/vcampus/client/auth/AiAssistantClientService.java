package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.ai.AiStreamListener;
import edu.seu.vcampus.common.ai.*;

import java.util.List;

/** AI 客户端边界；当前只保留接口阶段的能力占位。 */
public interface AiAssistantClientService {
    String query(String sessionId, String text, AiStreamListener listener);

    void cancel(String requestId);

    List<AiSessionSummary> sessions() throws ClientServiceException;
    String createSession() throws ClientServiceException;
    List<AiChatMessage> history(String sessionId) throws ClientServiceException;
    void clearSession(String sessionId) throws ClientServiceException;
    AiConfirmResult confirm(long actionId, boolean agreed) throws ClientServiceException;
    AiPage<AiKnowledgeChunk> knowledge(AiKnowledgeQuery query) throws ClientServiceException;
    AiKnowledgeChunk saveKnowledge(AiKnowledgeSaveRequest request) throws ClientServiceException;
    void deleteKnowledge(long chunkId) throws ClientServiceException;
    AiMonitorSnapshot monitor() throws ClientServiceException;
}
