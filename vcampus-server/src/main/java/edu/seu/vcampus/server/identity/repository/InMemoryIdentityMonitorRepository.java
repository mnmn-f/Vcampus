package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.MonitorSnapshotDto;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;

/** 内存系统监控仓储。 */
final class InMemoryIdentityMonitorRepository implements IdentityMonitorRepository {
    private final InMemoryIdentityState state;

    InMemoryIdentityMonitorRepository(InMemoryIdentityState state) { this.state = state; }

    @Override public MonitorSnapshotDto snapshot(Connection c) {
        synchronized (state) {
            int active = 0;
            for (IdentitySessionRecord session : state.sessions.values()) {
                if (session.getRevokedAt() == null && (session.getExpiresAt() == null
                        || session.getExpiresAt().isAfter(LocalDateTime.now()))) active++;
            }
            return new MonitorSnapshotDto(true, active, state.users.size(), LocalDateTime.now(),
                    "内存仓储正常");
        }
    }
}
