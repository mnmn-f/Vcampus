package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;

import java.sql.Connection;
import java.sql.SQLException;

/** 自习室和预约记录持久化边界。 */
public interface StudyRoomRepository {
    PageResult<StudyRoomView> search(Connection connection,
                                     StudyRoomSearchRequest request)
            throws SQLException;

    StudyRoomView findByIdForUpdate(Connection connection, long roomId)
            throws SQLException;

    StudyRoomView save(Connection connection, StudyRoomUpsertRequest request)
            throws SQLException;

}
