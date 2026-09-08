package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.MonitorSnapshotDto;

import java.sql.Connection;

/** 数据库健康和基础运行计数边界。 */
public interface IdentityMonitorRepository {
    MonitorSnapshotDto snapshot(Connection connection);
}
