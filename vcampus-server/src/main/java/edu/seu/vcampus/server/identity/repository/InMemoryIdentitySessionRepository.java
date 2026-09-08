package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.SessionDto;
import edu.seu.vcampus.common.dto.identity.SessionPage;
import edu.seu.vcampus.common.dto.identity.SessionQuery;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 会话内存仓储；强制下线同时撤销运行时 SessionManager 令牌。 */
final class InMemoryIdentitySessionRepository implements IdentitySessionRepository {
    private final InMemoryIdentityState state;
    private final SessionManager sessionManager;

    InMemoryIdentitySessionRepository(InMemoryIdentityState state, SessionManager sessionManager) {
        this.state = state;
        this.sessionManager = sessionManager;
    }

    @Override public SessionPage search(Connection c, SessionQuery query) {
        SessionQuery q = query == null ? new SessionQuery() : query;
        synchronized (state) {
            List<SessionDto> found = new ArrayList<SessionDto>();
            for (IdentitySessionRecord record : state.sessions.values()) {
                if (q.getUserId() != null && q.getUserId().longValue() != record.getUserId()) continue;
                if (!q.isIncludeRevoked() && record.getRevokedAt() != null) continue;
                if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(LocalDateTime.now())
                        && !q.isIncludeRevoked()) continue;
                found.add(record.toDto());
            }
            Collections.sort(found, new Comparator<SessionDto>() {
                @Override public int compare(SessionDto a, SessionDto b) {
                    int result = safeTime(b.getLastSeenAt()).compareTo(safeTime(a.getLastSeenAt()));
                    return result == 0 ? Long.compare(b.getId(), a.getId()) : result;
                }
            });
            int from = Math.min(q.getOffset(), found.size());
            int to = Math.min(from + q.getPageSize(), found.size());
            return new SessionPage(new ArrayList<SessionDto>(found.subList(from, to)), q.getPage(),
                    q.getPageSize(), found.size());
        }
    }

    @Override public boolean revoke(Connection c, long sessionId) {
        synchronized (state) {
            IdentitySessionRecord old = state.sessions.get(sessionId);
            if (old == null || old.getRevokedAt() != null) return false;
            revokeRuntime(old);
            state.sessions.put(sessionId, new IdentitySessionRecord(old.getId(), old.getUserId(),
                    old.getAccount(), old.getRawToken(), old.getActiveRole(), old.getCreatedAt(),
                    old.getLastSeenAt(), old.getExpiresAt(), LocalDateTime.now()));
            return true;
        }
    }

    @Override public int revokeUserSessions(Connection c, long userId) {
        synchronized (state) {
            int count = 0;
            for (IdentitySessionRecord record : new ArrayList<IdentitySessionRecord>(state.sessions.values())) {
                if (record.getUserId() == userId && record.getRevokedAt() == null) {
                    revokeRuntime(record);
                    state.sessions.put(record.getId(), new IdentitySessionRecord(record.getId(),
                            record.getUserId(), record.getAccount(), record.getRawToken(),
                            record.getActiveRole(), record.getCreatedAt(), record.getLastSeenAt(),
                            record.getExpiresAt(), LocalDateTime.now()));
                    count++;
                }
            }
            return count;
        }
    }

    @Override public int revokePersistedUserSessions(Connection c, long userId) {
        synchronized (state) {
            int count = 0;
            for (IdentitySessionRecord record
                    : new ArrayList<IdentitySessionRecord>(state.sessions.values())) {
                if (record.getUserId() == userId && record.getRevokedAt() == null) {
                    state.sessions.put(record.getId(), new IdentitySessionRecord(record.getId(),
                            record.getUserId(), record.getAccount(), record.getRawToken(),
                            record.getActiveRole(), record.getCreatedAt(), record.getLastSeenAt(),
                            record.getExpiresAt(), LocalDateTime.now()));
                    count++;
                }
            }
            return count;
        }
    }

    @Override public void observe(SessionContext session) {
        if (session == null) return;
        synchronized (state) {
            Long existing = state.idsByToken.get(session.getSessionToken());
            long id = existing == null ? state.sessionSequence++ : existing.longValue();
            LocalDateTime created = LocalDateTime.ofInstant(
                    org.threeten.bp.Instant.ofEpochMilli(session.getCreatedAt()),
                    org.threeten.bp.ZoneId.systemDefault());
            LocalDateTime seen = LocalDateTime.ofInstant(
                    org.threeten.bp.Instant.ofEpochMilli(session.getLastAccessAt()),
                    org.threeten.bp.ZoneId.systemDefault());
            IdentitySessionRecord record = new IdentitySessionRecord(id, session.getUserId(),
                    session.getAccount(), session.getSessionToken(), session.getActiveRole(), created,
                    seen, created.plusHours(24L), null);
            state.sessions.put(id, record);
            state.idsByToken.put(session.getSessionToken(), id);
        }
    }

    void add(SessionContext session) { observe(session); }

    private void revokeRuntime(IdentitySessionRecord record) {
        if (sessionManager != null && record.getRawToken() != null) {
            sessionManager.invalidate(record.getRawToken());
        }
    }

    private static LocalDateTime safeTime(LocalDateTime value) {
        return value == null ? LocalDateTime.MIN : value;
    }
}
