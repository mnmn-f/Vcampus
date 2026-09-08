package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.ai.*;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.AiCommands;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 网络模式 AI 客户端：普通管理请求复用 network，回答使用专用流式连接。 */
public final class NetworkAiAssistantClientService implements AiAssistantClientService {
    private final NetworkClientService network;
    private final ClientSession session;
    private final String host;
    private final int port;
    private final int connectTimeout;
    private final int readTimeout;
    private final Map<String, AiStreamConnection> active =
            new ConcurrentHashMap<String, AiStreamConnection>();

    public NetworkAiAssistantClientService(NetworkClientService network, ClientSession session,
            String host, int port, int connectTimeout, int readTimeout) {
        this.network = network; this.session = session; this.host = host; this.port = port;
        this.connectTimeout = connectTimeout; this.readTimeout = readTimeout;
    }

    public String query(String sessionId, String text, AiMode mode,
                        final AiStreamListener listener) {
        return query(sessionId, text, mode, null, listener);
    }

    public String query(String sessionId, String text, AiMode mode,
                        List<AiAttachment> attachments,
                        final AiStreamListener listener) {
        return queryWithRequestId(UUID.randomUUID().toString(), sessionId, text, mode,
                attachments, listener);
    }

    public String queryWithRequestId(String preferredRequestId, String sessionId, String text,
                        AiMode mode, List<AiAttachment> attachments,
                        final AiStreamListener listener) {
        final String requestId = preferredRequestId == null || preferredRequestId.trim().isEmpty()
                ? UUID.randomUUID().toString() : preferredRequestId.trim();
        final AiStreamConnection connection = new AiStreamConnection(host, port, connectTimeout,
                readTimeout, session.getSessionToken(),
                new AiQuery(requestId, sessionId, text, mode, attachments),
                new AiConversationListener() {
                    public void onChunk(String value) { listener.onChunk(value); }
                    public void onComplete() { active.remove(requestId); listener.onComplete(); }
                    public void onFailure(String value) { active.remove(requestId); listener.onFailure(value); }
                    public void onActionRequired(AiPendingAction action) {
                        if (listener instanceof AiConversationListener) {
                            ((AiConversationListener) listener).onActionRequired(action);
                        }
                    }
                    public void onEvidence(List<AiAnswerEvidence> evidence) {
                        if (listener instanceof AiConversationListener) {
                            ((AiConversationListener) listener).onEvidence(evidence);
                        }
                    }
                });
        active.put(requestId, connection);
        Thread thread = new Thread(connection, "vcampus-ai-stream-" + requestId.substring(0, 8));
        thread.setDaemon(true); thread.start(); return requestId;
    }

    public void cancel(String requestId) {
        AiStreamConnection connection = active.remove(requestId);
        if (connection != null) connection.cancel();
        try { request(AiCommands.CANCEL, new AiCancelRequest(requestId), Boolean.class); }
        catch (ClientServiceException ignored) { }
    }

    public boolean ping() throws ClientServiceException {
        return request(AiCommands.PING, null, Boolean.class).booleanValue();
    }

    public List<AiSessionSummary> sessions() throws ClientServiceException {
        return list(request(AiCommands.SESSION_LIST, null, List.class), AiSessionSummary.class);
    }
    public String createSession() throws ClientServiceException {
        return request(AiCommands.SESSION_CREATE, null, String.class);
    }
    public List<AiChatMessage> history(String id) throws ClientServiceException {
        return list(request(AiCommands.SESSION_HISTORY, new AiSessionRequest(id), List.class),
                AiChatMessage.class);
    }
    public void clearSession(String id) throws ClientServiceException {
        request(AiCommands.SESSION_CLEAR, new AiSessionRequest(id), Boolean.class);
    }
    public void renameSession(String id, String title) throws ClientServiceException {
        request(AiCommands.SESSION_RENAME, new AiSessionRenameRequest(id, title), Boolean.class);
    }
    public List<AiSessionSummary> archivedSessions() throws ClientServiceException {
        return list(request(AiCommands.SESSION_ARCHIVED, null, List.class), AiSessionSummary.class);
    }
    public void restoreSession(String id) throws ClientServiceException {
        request(AiCommands.SESSION_RESTORE, new AiSessionRequest(id), Boolean.class);
    }
    public AiConfirmResult confirm(long id, boolean agreed) throws ClientServiceException {
        return request(AiCommands.CONFIRM, new AiActionConfirmation(id, agreed), AiConfirmResult.class);
    }
    public AiPage<AiKnowledgeChunk> knowledge(AiKnowledgeQuery query) throws ClientServiceException {
        return castPage(request(AiCommands.KNOWLEDGE_LIST, query, AiPage.class));
    }
    public AiKnowledgeChunk saveKnowledge(AiKnowledgeSaveRequest request) throws ClientServiceException {
        return request(AiCommands.KNOWLEDGE_SAVE, request, AiKnowledgeChunk.class);
    }
    public void deleteKnowledge(long id) throws ClientServiceException {
        request(AiCommands.KNOWLEDGE_DELETE, new AiKnowledgeDeleteRequest(id), Boolean.class);
    }
    public AiKnowledgeTestResult testKnowledge(String question) throws ClientServiceException {
        return request(AiCommands.KNOWLEDGE_TEST, new AiKnowledgeTestRequest(question),
                AiKnowledgeTestResult.class);
    }
    public AiKnowledgeImportResult importKnowledge(AiKnowledgeBatchRequest value)
            throws ClientServiceException {
        return request(AiCommands.KNOWLEDGE_IMPORT, value, AiKnowledgeImportResult.class);
    }
    public List<AiKnowledgeVersion> knowledgeVersions(long chunkId) throws ClientServiceException {
        return list(request(AiCommands.KNOWLEDGE_VERSIONS,
                new AiKnowledgeVersionRequest(chunkId, null), List.class), AiKnowledgeVersion.class);
    }
    public AiKnowledgeChunk rollbackKnowledge(long chunkId, long versionId)
            throws ClientServiceException {
        return request(AiCommands.KNOWLEDGE_ROLLBACK,
                new AiKnowledgeVersionRequest(chunkId, Long.valueOf(versionId)), AiKnowledgeChunk.class);
    }
    public void saveFeedback(AiFeedbackRequest feedback) throws ClientServiceException {
        request(AiCommands.FEEDBACK_SAVE, feedback, Boolean.class);
    }
    public List<AiFeedbackEntry> feedback(AiFeedbackQuery query) throws ClientServiceException {
        return list(request(AiCommands.FEEDBACK_LIST, query, List.class), AiFeedbackEntry.class);
    }
    public void updateFeedback(AiFeedbackTriageRequest value) throws ClientServiceException {
        request(AiCommands.FEEDBACK_UPDATE, value, Boolean.class);
    }
    public List<AiToolStatus> toolStatuses() throws ClientServiceException {
        return list(request(AiCommands.TOOL_STATUS, null, List.class), AiToolStatus.class);
    }
    public AiToolRouteTestResult testToolRoute(String question) throws ClientServiceException {
        return request(AiCommands.TOOL_ROUTE_TEST, new AiToolRouteTestRequest(question),
                AiToolRouteTestResult.class);
    }
    public AiMonitorSnapshot monitor() throws ClientServiceException {
        return request(AiCommands.MONITOR, null, AiMonitorSnapshot.class);
    }

    private <T> T request(String command, Serializable payload, Class<T> type)
            throws ClientServiceException {
        try {
            Message response = network.request(command, payload); Object value = response.getPayload();
            if (!type.isInstance(value)) throw new ClientServiceException("AI.INVALID_RESPONSE",
                    "AI 服务返回了不兼容的数据");
            return type.cast(value);
        } catch (NetworkClientException ex) {
            throw new ClientServiceException(ex.getCode(), ex.getMessage(), ex);
        }
    }

    private <T> List<T> list(Object value, Class<T> type) throws ClientServiceException {
        List<?> source = (List<?>) value;
        for (Object item : source) if (!type.isInstance(item)) {
            throw new ClientServiceException("AI.INVALID_RESPONSE", "AI 列表数据不兼容");
        }
        @SuppressWarnings("unchecked") List<T> result = (List<T>) source; return result;
    }

    @SuppressWarnings("unchecked")
    private AiPage<AiKnowledgeChunk> castPage(AiPage<?> page) {
        return (AiPage<AiKnowledgeChunk>) page;
    }
}
