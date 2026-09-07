package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.ai.AiStreamListener;
import edu.seu.vcampus.common.ai.*;

import java.util.List;

/** AI 客户端边界；当前只保留接口阶段的能力占位。 */
public interface AiAssistantClientService {
    default String query(String sessionId, String text, AiStreamListener listener) {
        return query(sessionId, text, AiMode.QA, listener);
    }

    String query(String sessionId, String text, AiMode mode, AiStreamListener listener);

    default String query(String sessionId, String text, AiMode mode,
                         List<AiAttachment> attachments, AiStreamListener listener) {
        if (attachments != null && !attachments.isEmpty()) {
            throw new IllegalArgumentException("当前 AI 客户端不支持附件");
        }
        return query(sessionId, text, mode, listener);
    }

    void cancel(String requestId);
    default boolean ping() throws ClientServiceException { return true; }

    List<AiSessionSummary> sessions() throws ClientServiceException;
    String createSession() throws ClientServiceException;
    List<AiChatMessage> history(String sessionId) throws ClientServiceException;
    void clearSession(String sessionId) throws ClientServiceException;
    default void renameSession(String sessionId, String title) throws ClientServiceException {
        throw unsupported();
    }
    AiConfirmResult confirm(long actionId, boolean agreed) throws ClientServiceException;
    AiPage<AiKnowledgeChunk> knowledge(AiKnowledgeQuery query) throws ClientServiceException;
    AiKnowledgeChunk saveKnowledge(AiKnowledgeSaveRequest request) throws ClientServiceException;
    void deleteKnowledge(long chunkId) throws ClientServiceException;
    default AiKnowledgeTestResult testKnowledge(String question) throws ClientServiceException {
        throw unsupported();
    }
    default List<AiKnowledgeVersion> knowledgeVersions(long chunkId)
            throws ClientServiceException { throw unsupported(); }
    default AiKnowledgeChunk rollbackKnowledge(long chunkId, long versionId)
            throws ClientServiceException { throw unsupported(); }
    default void saveFeedback(AiFeedbackRequest request) throws ClientServiceException {
        throw unsupported();
    }
    default List<AiFeedbackEntry> feedback() throws ClientServiceException { throw unsupported(); }
    default List<AiToolStatus> toolStatuses() throws ClientServiceException { throw unsupported(); }
    AiMonitorSnapshot monitor() throws ClientServiceException;

    default ClientServiceException unsupported() {
        return new ClientServiceException("AI.UNAVAILABLE", "当前客户端未启用此 AI 功能");
    }
}
