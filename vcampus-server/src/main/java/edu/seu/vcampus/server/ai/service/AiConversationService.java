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

    /** 返回有界的最近对话，供多轮指代和模型回答使用。 */
    public String recentContext(SessionContext session, String id) {
        if (id == null) return "";
        List<AiChatMessage> messages = history(session, id);
        StringBuilder out = new StringBuilder();
        int start = Math.max(0, messages.size() - 12);
        for (int i = start; i < messages.size(); i++) {
            AiChatMessage message = messages.get(i);
            String content = message.getContent() == null ? "" : message.getContent().trim();
            if (content.length() > 800) content = content.substring(0, 800) + "…";
            out.append("USER".equals(message.getSenderType()) ? "用户：" : "助手：")
                    .append(content).append('\n');
            if (out.length() > 5000) return out.substring(out.length() - 5000);
        }
        return out.toString();
    }

    public void clear(final SessionContext session, final String id) {
        tx(new Work<Void>() {
            public Void run(Connection c) throws Exception {
                requireOwn(c, session, id); repository.archive(c, id, session.getUserId());
                return null;
            }
        });
    }

    public void rename(final SessionContext session, final String id, final String title) {
        final String clean = title == null ? "" : title.trim();
        if (clean.isEmpty() || clean.length() > 80) {
            throw new AiServiceException(ResultCodes.INVALID_INPUT,
                    "会话标题不能为空且不能超过 80 字");
        }
        tx(new Work<Void>() {
            public Void run(Connection c) throws Exception {
                requireOwn(c, session, id); repository.rename(c, id, session.getUserId(), clean);
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
