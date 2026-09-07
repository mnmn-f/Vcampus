package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.ai.AiMonitorSnapshot;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.repository.AiToolRecord;
import edu.seu.vcampus.server.ai.repository.AiToolRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;

import java.sql.Connection;

/** 工具调用审计、确认状态和监控计数。 */
public final class AiToolService {
    public static final long CONFIRM_TTL_MILLIS = 5L * 60L * 1000L;
    private final AiToolRepository repository;
    private final TransactionManager transactions;

    public AiToolService(AiToolRepository repository, TransactionManager transactions) {
        this.repository = repository; this.transactions = transactions;
    }

    public long create(final String sessionId, final String requestId,
                       final String toolName, final boolean write,
                       final String arguments, final long userId) {
        return tx(new Work<Long>() {
            public Long run(Connection c) throws Exception {
                return Long.valueOf(repository.create(c, sessionId, requestId, toolName,
                        write, arguments, write ? "CONFIRM_REQUIRED" : "REQUESTED", userId));
            }
        }).longValue();
    }

    public AiToolRecord pending(final long actionId, final long userId) {
        AiToolRecord record = tx(new Work<AiToolRecord>() {
            public AiToolRecord run(Connection c) throws Exception {
                return repository.find(c, actionId);
            }
        });
        if (record == null || record.getRequestedBy() != userId) {
            throw new AiServiceException(ResultCodes.NOT_FOUND, "待确认操作不存在");
        }
        if (!"CONFIRM_REQUIRED".equals(record.getStatus())) {
            throw new AiServiceException(ResultCodes.CONFLICT, "该操作已经处理");
        }
        if (System.currentTimeMillis() - record.getCreatedAt() > CONFIRM_TTL_MILLIS) {
            finish(actionId, "CANCELLED", "确认已超时", Long.valueOf(userId));
            throw new AiServiceException(ResultCodes.CONFLICT, "确认已超时，请重新发起");
        }
        boolean claimed = tx(new Work<Boolean>() {
            public Boolean run(Connection c) throws Exception {
                long cutoff = System.currentTimeMillis() - CONFIRM_TTL_MILLIS;
                return Boolean.valueOf(repository.claim(c, actionId, userId, cutoff));
            }
        }).booleanValue();
        if (!claimed) {
            throw new AiServiceException(ResultCodes.CONFLICT, "该操作已经处理");
        }
        return record;
    }

    public void finish(final long id, final String status, final String summary,
                       final Long confirmedBy) {
        tx(new Work<Void>() {
            public Void run(Connection c) throws Exception {
                repository.finish(c, id, status, summary, confirmedBy); return null;
            }
        });
    }

    public AiMonitorSnapshot monitor(final boolean configured, final String model,
                                     final long knowledgeCount) {
        return tx(new Work<AiMonitorSnapshot>() {
            public AiMonitorSnapshot run(Connection c) throws Exception {
                return new AiMonitorSnapshot(configured, model,
                        repository.tableCount(c, "ai_chat_sessions", "WHERE status='ACTIVE'"),
                        repository.tableCount(c, "ai_chat_messages", ""), knowledgeCount,
                        repository.count(c, "CONFIRM_REQUIRED"), repository.count(c, "SUCCEEDED"),
                        repository.count(c, "FAILED"),
                        repository.tableCount(c, "ai_chat_messages",
                                "WHERE created_at>=DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 HOUR)"),
                        repository.tableCount(c, "ai_tool_call_logs",
                                "WHERE created_at>=DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 HOUR)"),
                        repository.tableCount(c, "ai_tool_call_logs",
                                "WHERE status='FAILED' AND created_at>=DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 HOUR)"),
                        repository.tableCount(c, "ai_tool_call_logs",
                                "WHERE status='CONFIRM_REQUIRED' AND created_at<DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE)"),
                        repository.tableCount(c, "ai_answer_feedback", ""),
                        repository.tableCount(c, "ai_answer_feedback", "WHERE rating='UNHELPFUL'"));
            }
        });
    }

    private <T> T tx(final Work<T> work) {
        try {
            return transactions.execute(new TransactionWork<T>() {
                public T execute(Connection c) throws Exception { return work.run(c); }
            });
        } catch (AiServiceException ex) { throw ex; }
        catch (Exception ex) {
            throw new AiServiceException(ResultCodes.INTERNAL_ERROR, "AI 工具服务暂时不可用", ex);
        }
    }

    private interface Work<T> { T run(Connection connection) throws Exception; }
}
