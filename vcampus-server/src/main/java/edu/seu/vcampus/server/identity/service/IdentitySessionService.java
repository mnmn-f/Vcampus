package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.dto.identity.SessionPage;
import edu.seu.vcampus.common.dto.identity.SessionQuery;
import edu.seu.vcampus.common.dto.identity.SessionRevokeRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.sql.Connection;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** 系统管理员会话查询和强制下线用例。 */
final class IdentitySessionService {
    private final IdentityRecordRepository repository;
    private final IdentityTransactionRunner transactions;
    private final SessionManager sessionManager;

    IdentitySessionService(IdentityRecordRepository repository, IdentityTransactionRunner transactions,
                           SessionManager sessionManager) {
        this.repository = repository;
        this.transactions = transactions;
        this.sessionManager = sessionManager;
    }

    SessionPage search(SessionContext session, SessionQuery query) throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.USER_MANAGE);
        final SessionQuery safe = query == null ? new SessionQuery() : query;
        if (!safe.isIncludeRevoked()) return runtimePage(safe);
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<SessionPage>() {
                    @Override public SessionPage execute(Connection c) { return repository.search(c, safe); }
                });
    }

    boolean revoke(final SessionContext session, final SessionRevokeRequest request)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.USER_MANAGE);
        if (request == null || request.getSessionId() <= 0) {
            throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "会话编号不正确");
        }
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<Boolean>() {
                    @Override public Boolean execute(Connection c) throws IdentityServiceException {
                        boolean runtime = sessionManager.invalidateSession(request.getSessionId());
                        if (!runtime && !repository.revoke(c, request.getSessionId())) {
                            throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "会话不存在或已下线");
                        }
                        IdentityServiceSupport.audit(repository, c, session, "SESSION_REVOKE",
                                "SESSION", Long.valueOf(request.getSessionId()), null);
                        return Boolean.TRUE;
                    }
                });
    }

    void observe(SessionContext session) {
        if (session != null) repository.observe(session);
    }

    private SessionPage runtimePage(SessionQuery query) throws IdentityServiceException {
        List<edu.seu.vcampus.common.dto.identity.SessionDto> found =
                new ArrayList<edu.seu.vcampus.common.dto.identity.SessionDto>();
        for (SessionManager.SessionInfo info : sessionManager.snapshotSessions()) {
            if (query.getUserId() != null && query.getUserId().longValue() != info.getUserId()) continue;
            LocalDateTime created = local(info.getCreatedAt());
            found.add(new edu.seu.vcampus.common.dto.identity.SessionDto(info.getSessionId(),
                    info.getUserId(), info.getAccount(), info.getActiveRole(), created,
                    local(info.getLastAccessAt()), created.plusHours(24L), null));
        }
        int from = Math.min(query.getOffset(), found.size());
        int to = Math.min(from + query.getPageSize(), found.size());
        return new SessionPage(new ArrayList<edu.seu.vcampus.common.dto.identity.SessionDto>(
                found.subList(from, to)), query.getPage(), query.getPageSize(), found.size());
    }

    private static LocalDateTime local(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
}
