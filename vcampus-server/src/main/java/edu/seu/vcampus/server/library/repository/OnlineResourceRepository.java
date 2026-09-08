package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;

import java.sql.Connection;
import java.sql.SQLException;

/** 线上资源持久化边界。 */
public interface OnlineResourceRepository {
    OnlineResourceView findById(Connection connection, long resourceId)
            throws SQLException;

    PageResult<OnlineResourceView> search(Connection connection,
                                           OnlineResourceSearchRequest request)
            throws SQLException;

    OnlineResourceView save(Connection connection, OnlineResourceUpsertRequest request,
                            long publisherId) throws SQLException;
}
