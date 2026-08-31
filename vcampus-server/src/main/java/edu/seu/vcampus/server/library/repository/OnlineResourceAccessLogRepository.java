package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;

import java.sql.Connection;
import java.sql.SQLException;

/** 线上资源访问日志持久化边界。 */
public interface OnlineResourceAccessLogRepository {
    void append(Connection connection, OnlineResourceAccessLogDto log) throws SQLException;

    OnlineResourceAccessLogPage search(Connection connection,
                                       OnlineResourceAccessLogQuery query) throws SQLException;
}
