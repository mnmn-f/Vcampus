package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;

import java.sql.Connection;
import java.sql.SQLException;
import org.threeten.bp.LocalDate;

/** 住宿记录与入住/调宿/退宿申请边界。 */
public interface DormAccommodationRepository {
    AccommodationDto findCurrent(Connection connection, long studentUserId,
                                  boolean forUpdate) throws SQLException;

    AccommodationDto findById(Connection connection, long recordId, boolean forUpdate)
            throws SQLException;

    AccommodationDto assign(Connection connection, long studentUserId, long bedId,
                            LocalDate effectiveDate, long actorUserId) throws SQLException;

    AccommodationDto transfer(Connection connection, long studentUserId, long recordId,
                              long targetBedId, LocalDate effectiveDate, long actorUserId)
            throws SQLException;

    AccommodationDto checkout(Connection connection, long studentUserId, long recordId,
                              LocalDate effectiveDate, long actorUserId) throws SQLException;

    AccommodationRequestDto submitRequest(Connection connection, long studentUserId,
                                          String requestType, Long currentRecordId,
                                          Long requestedBedId, String reason)
            throws SQLException;

    DormPage<AccommodationRequestDto> listRequests(Connection connection, Long studentUserId,
                                                   DormPageQuery query) throws SQLException;

    AccommodationRequestDto lockRequest(Connection connection, long requestId)
            throws SQLException;

    AccommodationRequestDto finishRequest(Connection connection, long requestId,
                                          long reviewerId, boolean approved, String remark)
            throws SQLException;
}
