package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.ai.AiChatMessage;
import edu.seu.vcampus.common.ai.AiSessionSummary;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.repository.AiConversationRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;
import java.util.List;

/** AI 会话所有权、消息顺序与事务边界。 */
public final class AiConversationService {
    private final AiConversationRepository repository;
    private final TransactionManager transactions;
    private final String modelName;

    public AiConversationService(AiConversationRepository repository,
                                 TransactionManager transactions, String modelName) {
        this.repository = repository; this.transactions = transactions;
        this.modelName = modelName;
    }

    public String create(final SessionContext session) {
        return tx(new Work<String>() {
            public String run(Connection c) throws Exception {
                return repository.create(c, session.getUserId(), modelName);
            }
        });
    }

    public List<AiSessionSummary> list(final SessionContext session) {
        return tx(new Work<List<AiSessionSummary>>() {
            public List<AiSessionSummary> run(Connection c) throws Exception {
                return repository.list(c, session.getUserId());
            }
        });
    }

    public List<AiChatMessage> history(final SessionContext session, final String id) {
        return tx(new Work<List<AiChatMessage>>() {
            public List<AiChatMessage> run(Connection c) throws Exception {
                requireOwn(c, session, id); return repository.history(c, id);
            }
        });
    }

    public void clear(final SessionContext session, final String id) {
        tx(new Work<Void>() {
            public Void run(Connection c) throws Exception {
                requireOwn(c, session, id); repository.archive(c, id, session.getUserId());
                return null;
            }
        });
    }

    public String beginQuery(final SessionContext session, String requested,
                             final String requestId, final String text) {
        final String sessionId = requested == null || requested.trim().isEmpty()
                ? create(session) : requested.trim();
        tx(new Work<Void>() {
            public Void run(Connection c) throws Exception {
                requireOwn(c, session, sessionId);
                repository.append(c, sessionId, requestId, "USER", text, "COMPLETED");
                repository.titleFromFirstQuery(c, sessionId, text); return null;
            }
        });
        return sessionId;
    }

    public void saveAssistant(final String sessionId, final String requestId,
                              final String content, final String status) {
        tx(new Work<Void>() {
            public Void run(Connection c) throws Exception {
                repository.append(c, sessionId, requestId, "ASSISTANT", content, status);
                return null;
            }
        });
    }

    private void requireOwn(Connection c, SessionContext session, String id) throws Exception {
        if (!repository.owns(c, session.getUserId(), id)) {
            throw new AiServiceException(ResultCodes.NOT_FOUND, "会话不存在或已清空");
        }
    }

    private <T> T tx(final Work<T> work) {
        try {
            return transactions.execute(new TransactionWork<T>() {
                public T execute(Connection c) throws Exception { return work.run(c); }
            });
        } catch (AiServiceException ex) { throw ex; }
        catch (Exception ex) {
            throw new AiServiceException(ResultCodes.INTERNAL_ERROR, "AI 会话服务暂时不可用", ex);
        }
    }

    private interface Work<T> { T run(Connection connection) throws Exception; }
}
