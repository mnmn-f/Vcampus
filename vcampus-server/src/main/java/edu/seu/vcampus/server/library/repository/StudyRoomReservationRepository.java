package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;

import java.sql.Connection;
import java.sql.SQLException;
import org.threeten.bp.LocalDateTime;

/** 自习室预约持久化边界。 */
public interface StudyRoomReservationRepository {
    PageResult<StudyRoomReservationView> search(Connection connection,
                                                 StudyRoomReservationSearchRequest request,
                                                 Long userId) throws SQLException;

    StudyRoomReservationView findById(Connection connection, long reservationId)
            throws SQLException;

    StudyRoomReservationView findByIdForUpdate(Connection connection,
                                                         long reservationId)
            throws SQLException;

    boolean hasRoomOverlap(Connection connection, long roomId,
                           LocalDateTime startAt, LocalDateTime endAt)
            throws SQLException;

    boolean hasUserOverlap(Connection connection, long userId,
                           LocalDateTime startAt, LocalDateTime endAt)
            throws SQLException;

    StudyRoomReservationView insert(Connection connection, long roomId, long userId,
                                    LocalDateTime startAt, LocalDateTime endAt)
            throws SQLException;

    boolean cancel(Connection connection, long reservationId,
                   LocalDateTime cancelledAt) throws SQLException;
}
