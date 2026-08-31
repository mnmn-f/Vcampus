package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.SessionPage;
import edu.seu.vcampus.common.dto.identity.SessionQuery;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;

/** user_sessions 与当前运行时会话撤销边界。 */
public interface IdentitySessionRepository {
    SessionPage search(Connection connection, SessionQuery query);
    boolean revoke(Connection connection, long sessionId);
    int revokeUserSessions(Connection connection, long userId);

    /** 仅更新持久化会话状态；运行时令牌由事务提交后另行清理。 */
    int revokePersistedUserSessions(Connection connection, long userId);

    void observe(SessionContext session);
}
