package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;

import java.sql.Connection;
import java.sql.SQLException;

/** 教室及预约申请持久化边界。 */
public interface CampusClassroomRepository {
    CampusPage<CampusClassroomDto> listClassrooms(Connection connection, CampusPageQuery query)
            throws SQLException;
    CampusClassroomDto findClassroom(Connection connection, long id) throws SQLException;
    CampusClassroomDto lockClassroom(Connection connection, long id) throws SQLException;
    CampusPage<ClassroomReservationDto> listReservations(Connection connection, Long applicantId,
                                                         CampusPageQuery query) throws SQLException;
    ClassroomReservationDto findReservation(Connection connection, long id) throws SQLException;
    ClassroomReservationDto lockReservation(Connection connection, long id) throws SQLException;
    ClassroomReservationDto createReservation(Connection connection,
                                              ClassroomReservationRequest request,
                                              long applicantId) throws SQLException;
    ClassroomReservationDto review(Connection connection, ClassroomReviewRequest request,
                                   long reviewerId) throws SQLException;
    ClassroomReservationDto cancelReservation(Connection connection, long reservationId, long applicantId)
            throws SQLException;
    boolean hasConflict(Connection connection, long classroomId,
                        org.threeten.bp.LocalDateTime startAt, org.threeten.bp.LocalDateTime endAt,
                        long excludingReservationId) throws SQLException;
    boolean hasApplicantConflict(Connection connection, long applicantId,
                                 org.threeten.bp.LocalDateTime startAt, org.threeten.bp.LocalDateTime endAt)
            throws SQLException;
}
