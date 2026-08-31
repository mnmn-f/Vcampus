package edu.seu.vcampus.server.security;

import edu.seu.vcampus.common.security.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** 服务器端会话注册表；客户端只能提交令牌，不能改变其中的身份信息。 */
public final class SessionManager {
    private final ConcurrentMap<String, SessionContext> sessions =
            new ConcurrentHashMap<String, SessionContext>();
    private final ConcurrentMap<String, Long> idsByToken =
            new ConcurrentHashMap<String, Long>();
    private final ConcurrentMap<Long, String> tokensById =
            new ConcurrentHashMap<Long, String>();
    private final AtomicLong sessionIdSequence = new AtomicLong(1L);

    public SessionContext createSession(long userId, String account,
                                        String displayName, Set<Role> roles) {
        Set<Role> safeRoles = roles == null
                ? Collections.<Role>emptySet() : roles;
        EnumSet<Role> orderedRoles = safeRoles.isEmpty()
                ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(safeRoles);
        Role activeRole = orderedRoles.isEmpty() ? null : orderedRoles.iterator().next();
        return createSession(userId, account, displayName, orderedRoles, activeRole);
    }

    public SessionContext createSession(long userId, String account,
                                        String displayName, Set<Role> roles,
                                        Role activeRole) {
        String token = UUID.randomUUID().toString();
        SessionContext context = new SessionContext(
                token, userId, account, displayName, roles, activeRole);
        sessions.put(token, context);
        long sessionId = sessionIdSequence.getAndIncrement();
        idsByToken.put(token, Long.valueOf(sessionId));
        tokensById.put(Long.valueOf(sessionId), token);
        return context;
    }

    public SessionContext find(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return null;
        }
        return sessions.get(sessionToken);
    }

    /** 查找并刷新最后访问时间；路由器应使用该方法。 */
    public SessionContext resolve(String sessionToken) {
        SessionContext result = find(sessionToken);
        if (result != null) {
            result.touch();
        }
        return result;
    }

    public boolean contains(String sessionToken) {
        return find(sessionToken) != null;
    }

    public boolean switchActiveRole(String sessionToken, Role role) {
        if (sessionToken == null || sessionToken.trim().isEmpty() || role == null) {
            return false;
        }
        while (true) {
            SessionContext current = sessions.get(sessionToken);
            if (current == null || !current.hasRole(role)) {
                return false;
            }
            SessionContext replacement = current.withActiveRole(role);
            if (sessions.replace(sessionToken, current, replacement)) {
                return true;
            }
        }
    }

    public void invalidate(String sessionToken) {
        if (sessionToken != null) {
            sessions.remove(sessionToken);
            Long id = idsByToken.remove(sessionToken);
            if (id != null) tokensById.remove(id);
        }
    }

    /** 管理端使用的脱敏在线会话快照，不包含 raw token。 */
    public List<SessionInfo> snapshotSessions() {
        List<SessionInfo> result = new ArrayList<SessionInfo>();
        for (java.util.Map.Entry<String, SessionContext> entry : sessions.entrySet()) {
            Long id = idsByToken.get(entry.getKey());
            if (id != null) result.add(new SessionInfo(id.longValue(), entry.getValue()));
        }
        java.util.Collections.sort(result, new Comparator<SessionInfo>() {
            @Override public int compare(SessionInfo a, SessionInfo b) {
                return Long.compare(b.getSessionId(), a.getSessionId());
            }
        });
        return result;
    }

    /** snapshotSessions 的兼容命名；返回值仍不包含令牌。 */
    public List<SessionInfo> snapshot() { return snapshotSessions(); }

    /** 按脱敏会话编号撤销，并立即移除路由可解析的令牌。 */
    public boolean invalidateSession(long sessionId) {
        String token = tokensById.get(Long.valueOf(sessionId));
        if (token == null) return false;
        invalidate(token);
        return true;
    }

    /** invalidateSession 的兼容命名。 */
    public boolean invalidateById(long sessionId) { return invalidateSession(sessionId); }

    /** 撤销指定用户的全部运行时会话，返回实际撤销数。 */
    public int invalidateUserSessions(long userId) {
        int count = 0;
        for (SessionInfo info : snapshotSessions()) {
            if (info.getUserId() == userId && invalidateSession(info.getSessionId())) count++;
        }
        return count;
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    public void invalidateAll() {
        sessions.clear();
        idsByToken.clear();
        tokensById.clear();
    }

    /** 只暴露管理端所需的身份与时间摘要。 */
    public static final class SessionInfo {
        private final long sessionId;
        private final SessionContext context;

        private SessionInfo(long sessionId, SessionContext context) {
            this.sessionId = sessionId;
            this.context = context;
        }

        public long getSessionId() { return sessionId; }
        public long getUserId() { return context.getUserId(); }
        public String getAccount() { return context.getAccount(); }
        public Role getActiveRole() { return context.getActiveRole(); }
        public long getCreatedAt() { return context.getCreatedAt(); }
        public long getLastAccessAt() { return context.getLastAccessAt(); }
    }
}
